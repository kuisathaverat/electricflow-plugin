package org.jenkinsci.plugins.electricflow;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class Credential extends AbstractDescribableImpl<Credential> {

  private String credentialId;

  @DataBoundConstructor
  public Credential(String credentialId) {
    this.credentialId = credentialId;
  }

  private static StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(
      String credentialsId) {
    if (credentialsId == null) {
      return null;
    }

    return CredentialsMatchers.firstOrNull(
        lookupCredentials(
            StandardUsernamePasswordCredentials.class,
            Jenkins.get(),
            ACL.SYSTEM,
            new SchemeRequirement("http"),
            new SchemeRequirement("https")),
        CredentialsMatchers.withId(credentialsId));
  }

  private static StandardUsernamePasswordCredentials
      getStandardUsernamePasswordCredentialsByIdAndRun(String credentialsId, Run run) {
    if (credentialsId == null) {
      return null;
    }

    return CredentialsProvider.findCredentialById(
        credentialsId, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
  }

  public String getCredentialId(EnvReplacer envReplacer) {
    return envReplacer == null ? getCredentialId() : envReplacer.expandEnv(getCredentialId());
  }

  public String getCredentialId() {
    return credentialId;
  }

  public StandardUsernamePasswordCredentials getUsernamePasswordBasedOnCredentialId(
      EnvReplacer envReplacer, Run run) {
    String credentialIdResolved = getCredentialId(envReplacer);
    return run == null
        ? getStandardUsernamePasswordCredentialsById(credentialIdResolved)
        : getStandardUsernamePasswordCredentialsByIdAndRun(credentialIdResolved, run);
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Credential> {

    public static ListBoxModel doFillCredentialIdItems(Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      if (!item.hasPermission(Item.EXTENDED_READ)
          && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
        return new ListBoxModel();
      }

      return new StandardUsernameListBoxModel()
          .includeEmptyValue()
          .includeMatchingAs(
              ACL.SYSTEM,
              Jenkins.get(),
              StandardUsernamePasswordCredentials.class,
              Collections.emptyList(),
              CredentialsMatchers.always());
    }

    @Override
    public String getDisplayName() {
      return "Credential";
    }
  }
}
