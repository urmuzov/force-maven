package com.sforce.contrib.connection;

import com.sforce.contrib.partner.Context;

/**
 * User: urmuzov
 * Date: 11/1/13
 * Time: 4:53 PM
 */
public class ConnectionDto {
    private String sessionId;
    private String serviceEndpoint;
    private String organizationId;
    private String organizationName;
    private String profileId;
    private String roleId;
    private int sessionSecondsValid;
    private String userEmail;
    private String userFullName;
    private String userId;
    private String userName;
    private Context context;

    public ConnectionDto() {
    }

    public ConnectionDto(String sessionId, String serviceEndpoint, String organizationId, String organizationName, String profileId, String roleId, int sessionSecondsValid, String userEmail, String userFullName, String userId, String userName, Context context) {
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.profileId = profileId;
        this.roleId = roleId;
        this.serviceEndpoint = serviceEndpoint;
        this.sessionId = sessionId;
        this.sessionSecondsValid = sessionSecondsValid;
        this.userEmail = userEmail;
        this.userFullName = userFullName;
        this.userId = userId;
        this.userName = userName;
        this.context = context;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getSessionSecondsValid() {
        return sessionSecondsValid;
    }

    public void setSessionSecondsValid(int sessionSecondsValid) {
        this.sessionSecondsValid = sessionSecondsValid;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConnectionDto{");
        sb.append("sessionId='").append(sessionId).append('\'');
        sb.append(", serviceEndpoint='").append(serviceEndpoint).append('\'');
        sb.append(", organizationId='").append(organizationId).append('\'');
        sb.append(", organizationName='").append(organizationName).append('\'');
        sb.append(", profileId='").append(profileId).append('\'');
        sb.append(", roleId='").append(roleId).append('\'');
        sb.append(", sessionSecondsValid=").append(sessionSecondsValid);
        sb.append(", userEmail='").append(userEmail).append('\'');
        sb.append(", userFullName='").append(userFullName).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", context=").append(context);
        sb.append('}');
        return sb.toString();
    }
}
