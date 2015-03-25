package com.sforce.contrib.partner;

public class CustomSettings extends SObjectType {
    private CustomSettingsVisibility visibility;
    private Field<String> setupOwnerId;

    public void init(Package pkg, String sfName, String label, String pluralLabel, CustomSettingsVisibility visibility) {
        super.init(pkg, sfName, label, pluralLabel);
        this.visibility = visibility;
        this.setupOwnerId = new Field<String>(this, "setupOwnerId", "SetupOwnerId", "Setup Owner Id", FieldType.LOOKUP, null, null, null, false, false);
        addField(setupOwnerId);
    }

    public Field<String> setupOwnerId() {
        return setupOwnerId;
    }

    public CustomSettingsVisibility visibility() {
        return visibility;
    }
}
