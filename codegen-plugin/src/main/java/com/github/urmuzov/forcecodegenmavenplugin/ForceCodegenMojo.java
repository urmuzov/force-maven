package com.github.urmuzov.forcecodegenmavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @goal generate-partner-metadata
 * @phase compile
 */
public class ForceCodegenMojo extends AbstractMojo {
    /**
     * @parameter expression="false"
     * @required
     */
    private boolean debug = false;
    /**
     * @parameter
     * @required
     */
    private File objectsDir = null;
    /**
     * @parameter
     * @required
     */
    private String outputPackage = null;
    /**
     * @parameter
     */
    private String customObjectPrefix = null;
    /**
     * @parameter expression="__c"
     */
    private String customObjectSuffix = null;
    /**
     * @parameter
     */
    private boolean customObjectCapitalize;
    /**
     * @parameter
     */
    private String customFieldPrefix = null;
    /**
     * @parameter expression="__c"
     */
    private String customFieldSuffix = null;
    /**
     * @parameter
     */
    private boolean customFieldCapitalize;
    /**
     * @parameter expression="${project.build.directory}/generated-sources/force-maven"
     * @required
     */
    private String outputDir = null;
    /**
     * @parameter default-value="${project}"
     */
    private MavenProject mavenProject;

    private VelocityEngine ve;
    private String outDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (objectsDir == null || !objectsDir.exists() || !objectsDir.isDirectory()) {
                throw new MojoExecutionException("Incorrect 'objectsDir' parameter");
            }
            if (outputPackage == null) {
                throw new MojoExecutionException("Incorrect 'outputPackage' parameter");
            }
            if (outputDir == null) {
                throw new MojoExecutionException("Incorrect 'outputDir' parameter");
            }
            customFieldPrefix = (customFieldPrefix == null || customFieldPrefix.equals("empty")) ? "" : customFieldPrefix;
            customFieldSuffix = (customFieldSuffix == null || customFieldSuffix.equals("empty")) ? "" : customFieldSuffix;
            customObjectPrefix = (customObjectPrefix == null || customObjectPrefix.equals("empty")) ? "" : customObjectPrefix;
            customObjectSuffix = (customObjectSuffix == null || customObjectSuffix.equals("empty")) ? "" : customObjectSuffix;
            infoIfDebug("debug: " + debug);
            infoIfDebug("objectsDir: " + objectsDir);
            infoIfDebug("outputPackage: " + outputPackage);
            infoIfDebug("customObjectPrefix: " + customObjectPrefix);
            infoIfDebug("customObjectSuffix: " + customObjectSuffix);
            infoIfDebug("customObjectCapitalize: " + customObjectCapitalize);
            infoIfDebug("customFieldPrefix: " + customFieldPrefix);
            infoIfDebug("customFieldSuffix: " + customFieldSuffix);
            infoIfDebug("customFieldCapitalize: " + customFieldCapitalize);

            String outputPackageDir = outputPackage.replace(".", "/");
            outDir = outputDir + "/" + outputPackageDir;
            File dir = new File(outDir);
            dir.mkdirs();

            Log log = getLog();

            ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            ve.init();

            VelocityContext vc = new VelocityContext();
            vc.put("outputPackage", outputPackage);

            merge("Field.java.vm", "Field.java", vc);
            merge("FieldType.java.vm", "FieldType.java", vc);
            merge("Fields.java.vm", "Fields.java", vc);
            merge("CustomSettings.java.vm", "CustomSettings.java", vc);
            merge("CustomSettingsVisibility.java.vm", "CustomSettingsVisibility.java", vc);
            merge("StandardCase.java.vm", "StandardCase.java", vc);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            List<String> objectNames = new ArrayList<String>();
            for (File objectFile : objectsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".object");
                }
            })) {
                String objectName = objectFile.getName();
                objectName = objectName.replace(".object", "");
                String newObjectName = convertObjectName(objectName);
                vc.put("objectName", newObjectName);
                vc.put("objectApiName", objectName);
                objectNames.add(newObjectName);
                Document document = builder.parse(objectFile);
                List<Node> fields = getElementsByTagName(document.getDocumentElement(), "fields");
                List<String> fieldNames = new ArrayList<String>();
                Map<String, String> oldNamesMap = new HashMap<String, String>();
                Map<String, String> typeEnumMap = new HashMap<String, String>();
                Map<String, String> typeClassMap = new HashMap<String, String>();
                Map<String, String> lengthMap = new HashMap<String, String>();
                for (Node field : fields) {
                    Node fullNameNode = getFirstElementsByTagName(field, "fullName");
                    if (fullNameNode == null) {
                        throw new MojoExecutionException("Parsing error: fullName is null (" + objectName + ")");
                    }
                    String fullName = fullNameNode.getTextContent();
                    String newFullName = convertFieldName(fullName);
                    if (!checkCustomFieldName(newFullName)) {
                        throw new MojoExecutionException("Parsing error: new fullName clashes standard name (" + newFullName + ") for object (" + objectName + ")");
                    }
                    oldNamesMap.put(newFullName, fullName);
                    Node typeNode = getFirstElementsByTagName(field, "type");
                    if (typeNode == null) {
                        throw new MojoExecutionException("Parsing error: type is null for field (" + fullName + ") for object (" + objectName + ")");
                    }
                    String type = typeNode.getTextContent();
                    Node lengthNode = getFirstElementsByTagName(field, "length");
                    String length = lengthNode == null ? null : lengthNode.getTextContent();
                    if (length != null) {
                        lengthMap.put(newFullName, length);
                    }
                    boolean formula = getFirstElementsByTagName(field, "formula") != null;
                    if (type.equals("Text")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("Number")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "DOUBLE");
                        typeClassMap.put(newFullName, "Double");
                    } else if (type.equals("Percent")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "DOUBLE");
                        typeClassMap.put(newFullName, "Double");
                    } else if (type.equals("MasterDetail")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("Lookup")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("Checkbox")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "BOOLEAN");
                        typeClassMap.put(newFullName, "Boolean");
                    } else if (type.equals("Picklist")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("LongTextArea")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("DateTime")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "CALENDAR");
                        typeClassMap.put(newFullName, "Calendar");
                    } else if (type.equals("Date")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "CALENDAR");
                        typeClassMap.put(newFullName, "Calendar");
                    } else if (type.equals("Currency")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "DOUBLE");
                        typeClassMap.put(newFullName, "Double");
                    } else if (type.equals("TextArea")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("EncryptedText")) {
                        fieldNames.add(newFullName);
                        typeEnumMap.put(newFullName, "STRING");
                        typeClassMap.put(newFullName, "String");
                    } else if (type.equals("Summary")) {
                        // skipping
                    } else {
                        throw new MojoExecutionException("Parsing error: unknown type (" + type + ") for field (" + fullName + ") for object (" + objectName + ")");
                    }
                    if (formula) {
                        typeEnumMap.put(newFullName, "FORMULA");
                    }
                }
                vc.put("fieldNames", fieldNames);
                vc.put("oldNamesMap", oldNamesMap);
                vc.put("typeEnumMap", typeEnumMap);
                vc.put("typeClassMap", typeClassMap);
                vc.put("lengthMap", lengthMap);
                if (!getElementsByTagName(document.getDocumentElement(), "customSettingsType").isEmpty()) {
                    Node visibility = getFirstElementsByTagName(document.getDocumentElement(), "customSettingsVisibility");
                    vc.put("visibility", visibility != null && visibility.getTextContent().equals("Protected") ? "PROTECTED" : "PUBLIC");
                    merge("CustomSettingsExt.java.vm", newObjectName + ".java", vc);
                } else {
                    String superClass = "Fields";
                    if (objectName.equals("Case")) {
                        superClass = "StandardCase";
                    }
                    vc.put("superClass", superClass);
                    merge("FieldsExt.java.vm", newObjectName + ".java", vc);
                }
            }
            vc.put("objectNames", objectNames);
            merge("Objects.java.vm", "Objects.java", vc);
        } catch (IOException e) {
            throw new MojoExecutionException("IOException", e);
        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException("ParserConfigurationException", e);
        } catch (SAXException e) {
            throw new MojoExecutionException("SAXException", e);
        }
    }

    public void info(String message) {
        getLog().info(message);
    }

    public void infoIfDebug(String message) {
        if (debug) {
            info(message);
        }
    }

    public void merge(String templateName, String outputName, VelocityContext vc) throws IOException {
        Template fieldClassTemplate = ve.getTemplate(templateName, "utf-8");
        FileWriter writer = new FileWriter(outDir + "/" + outputName);
        fieldClassTemplate.merge(vc, writer);
        writer.flush();
        writer.close();
    }

    public List<Node> getElementsByTagName(Node node, String tagName) {
        List<Node> out = new ArrayList<Node>();
        NodeList nodes = node.getChildNodes();
        Node n;
        for (int i = 0; i < nodes.getLength(); i++) {
            n = nodes.item(i);
            if (n.getNodeName().equals(tagName)) {
                out.add(n);
            }
        }
        return out;
    }

    public Node getFirstElementsByTagName(Node node, String tagName) {
        NodeList nodes = node.getChildNodes();
        Node n;
        for (int i = 0; i < nodes.getLength(); i++) {
            n = nodes.item(i);
            if (n.getNodeName().equals(tagName)) {
                return n;
            }
        }
        return null;
    }

    public boolean isCustom(String name) {
        return name != null && name.endsWith("__c");
    }

    public String removeSalesforceSuffix(String name) {
        return isCustom(name) ? name.substring(0, name.length() - 3) : name;
    }

    public String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public String convertObjectName(String salesforceName) {
        if (isCustom(salesforceName)) {
            String name = removeSalesforceSuffix(salesforceName);
            if (customObjectCapitalize) {
                name = capitalize(name);
            }
            return customObjectPrefix + name + customObjectSuffix;
        }
        return salesforceName;
    }

    public String convertFieldName(String salesforceName) {
        if (isCustom(salesforceName)) {
            String name = removeSalesforceSuffix(salesforceName);
            if (customFieldCapitalize) {
                name = capitalize(name);
            }
            return customFieldPrefix + name + customFieldSuffix;
        }
        return salesforceName;
    }

    public boolean checkCustomFieldName(String name) {
        return name != null
                && !name.equalsIgnoreCase("Id")
                && !name.equalsIgnoreCase("Name")
                && !name.equalsIgnoreCase("Owner")
                && !name.equalsIgnoreCase("CreatedBy")
                && !name.equalsIgnoreCase("LastModifiedBy");
    }
}