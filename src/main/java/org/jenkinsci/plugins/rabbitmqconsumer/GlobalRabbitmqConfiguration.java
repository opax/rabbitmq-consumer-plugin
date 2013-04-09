package org.jenkinsci.plugins.rabbitmqconsumer;

import hudson.Extension;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.rabbitmq.client.ConnectionFactory;

import jenkins.model.GlobalConfiguration;

/**
 * Descriptor for global configuration.
 * 
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public final class GlobalRabbitmqConfiguration extends GlobalConfiguration {

    private static final String PLUGIN_NAME = Messages.PluginName();
    /**
     * The string in global configuration that indicates content is empty.
     */
    public static final String CONTENT_NONE = "-";

    private static final Logger LOGGER = Logger.getLogger(GlobalRabbitmqConfiguration.class.getName());
    private static final String[] AMQP_SCHEMES = { "amqp", "amqps" };
    private final UrlValidator urlValidator = new UrlValidator(AMQP_SCHEMES, UrlValidator.ALLOW_LOCAL_URLS);

    private boolean enableConsumer;
    private String serviceUri;
    private List<RabbitmqConsumeItem> consumeItems;
    private boolean enableDebug;

    /**
     * Creates GlobalRabbitmqConfiguration instance with specified parameters.
     * 
     * @param enableConsumer
     *            if this feature is enabled.
     * @param serviceUri
     *            the service URI.
     * @param consumeItems
     *            the list of consumer items.
     * @param enableDebug
     *            true if debug is enabled.
     */
    @DataBoundConstructor
    public GlobalRabbitmqConfiguration(boolean enableConsumer, String serviceUri,
            List<RabbitmqConsumeItem> consumeItems, boolean enableDebug) {
        this.enableConsumer = enableConsumer;
        this.serviceUri = StringUtils.strip(StringUtils.stripToNull(serviceUri), "/");
        this.consumeItems = consumeItems;
        this.enableDebug = enableDebug;
    }

    /**
     * Create GlobalRabbitmqConfiguration from disk.
     */
    public GlobalRabbitmqConfiguration() {
        load();
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        req.bindJSON(this, json);

        if (urlValidator.isValid(serviceUri)) {
            save();
            return true;
        }
        return false;
    }

    /**
     * Gets whether debug is enabled or not.
     * 
     * @return true if debug is enabled.
     */
    public boolean isEnableDebug() {
        return enableDebug;
    }

    /**
     * Sets flag whether debug is enabled or not.
     * 
     * @param enableDebug true if debug is enabled.
     */
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }

    /**
     * Gets whether this plugin is enabled or not.
     * 
     * @return true if this plugin is enabled.
     */
    public boolean isEnableConsumer() {
        return enableConsumer;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     * 
     * @param true if this plugin is enabled.
     */
    public void setEnableConsumer(boolean enableConsumer) {
        this.enableConsumer = enableConsumer;
    }

    /**
     * Gets URI for RabbitMQ service.
     * 
     * @return the URI.
     */
    public String getServiceUri() {
        return serviceUri;
    }

    /**
     * Sets URI for RabbitMQ service.
     * 
     * @param serviceUri
     *            the URI.
     */
    public void setServiceUri(final String serviceUri) {
        this.serviceUri = StringUtils.strip(StringUtils.stripToNull(serviceUri), "/");
    }

    /**
     * Checks given URI is valid.
     * 
     * @param value
     *            the URI.
     * @return FormValidation object that indicates ok or error.
     */
    public FormValidation doCheckServiceUri(@QueryParameter String value) {
        String val = StringUtils.stripToNull(value);
        if (val == null) {
            return FormValidation.ok();
        }

        if (urlValidator.isValid(val)) {
            return FormValidation.ok();
        } else {
            return FormValidation.error(Messages.InvalidURI());
        }
    }

    /**
     * Tests connection to given URI.
     * 
     * @param serviceUri
     *            the URI.
     * @return FormValidation object that indicates ok or error.
     * @throws ServletException
     *             exception for servlet.
     */
    public FormValidation doTestConnection(@QueryParameter("serviceUri") String serviceUri) throws ServletException {
        String uri = StringUtils.strip(StringUtils.stripToNull(serviceUri), "/");
        if (uri != null && urlValidator.isValid(uri)) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(uri);
                factory.newConnection();
                return FormValidation.ok(Messages.Success());
            } catch (URISyntaxException e) {
                return FormValidation.error(Messages.Error() + ": " + e);
            } catch (GeneralSecurityException e) {
                return FormValidation.error(Messages.Error() + ": " + e);
            } catch (IOException e) {
                return FormValidation.error(Messages.Error() + ": " + e);
            }
        }
        return FormValidation.error("Invalid URI");
    }

    /**
     * Gets the list of {@link RabbitmqConsumeItem}.
     * 
     * @return the list of {@link RabbitmqConsumeItem}
     */
    public List<RabbitmqConsumeItem> getConsumeItems() {
        return consumeItems;
    }

    /**
     * Sets the list of {@link RabbitmqConsumeItem}.
     * 
     * @param consumeItems
     *            the list of {@link RabbitmqConsumeItem}
     */
    public void setConsumeItems(List<RabbitmqConsumeItem> consumeItems) {
        this.consumeItems = consumeItems;
    }

    /**
     * Gets connection to service is established. Note that this is called by
     * Ajax.
     * 
     * @return true if connection is established.
     */
    @JavaScriptMethod
    public boolean isOpen() {
        LOGGER.info("#isOpen");
        return RMQManager.getInstance().isOpen();
    }

    /**
     * Gets specified queue is consumed or not. Note that this is called by
     * Ajax.
     * 
     * @param queueName
     *            the queue name.
     * @return true if specified queue is already consumed.
     */
    @JavaScriptMethod
    public boolean isConsume(String queueName) {
        LOGGER.info("#isConsume: " + queueName);
        RMQManager manager = RMQManager.getInstance();
        if (manager.isOpen()) {
            return manager.getChannelStatus(queueName);
        }
        return false;
    }

    /**
     * Gets this extension's instance.
     * 
     * @return the instance of this extension.
     */
    public static GlobalRabbitmqConfiguration get() {
        return GlobalConfiguration.all().get(GlobalRabbitmqConfiguration.class);
    }
}
