package com.sforce.contrib.partner;

public class CustomSettings extends SObjectType {
    private CustomSettingsVisibility visibility;
    private Field<String> setupOwnerId;

    public void init(Package pkg, String sfName, CustomSettingsVisibility visibility) {
        super.init(pkg, sfName);
        this.visibility = visibility;
        this.setupOwnerId = new Field<String>(this, "setupOwnerId", "SetupOwnerId", FieldType.LOOKUP, null, null, null, false, false);
        addField(setupOwnerId);
    }

    public Field<String> setupOwnerId() {
        return setupOwnerId;
    }

    public CustomSettingsVisibility visibility() {
        return visibility;
    }
}
