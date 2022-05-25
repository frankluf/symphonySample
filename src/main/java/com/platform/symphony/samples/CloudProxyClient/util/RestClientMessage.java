/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class RestClientMessage {
    public static String defaultBundleName = "com.platform.symphony.samples.CloudProxyClient.msg.messages";
    private static Map<Locale, ResourceBundle> bundleCache = new ConcurrentHashMap<Locale, ResourceBundle>();

    public static Logger log = Logger.getLogger(RestClientMessage.class);

    public RestClientMessage()
    {
    }

    /**
     * Gets the internationalized message based on the value of the key and the
     * appropriate <code>ResourceBundle</code> for the desired
     * <code>Locale</code>. The message is formatted using a
     * <code>MessageFormat</code> object to substitute the supplied set of
     * values into the message.
     *
     * @param pKey
     *            - message key
     * @param pArgs
     *            - message arguments
     * @return    - message in the appropriated locale, or message key if the
     *         message doesn't exist in the resource bundle
     */
    public static String getMessage(String key, Object... params)
    {
        try
        {
            ResourceBundle bundle = loadResourceBundle(defaultBundleName, Locale.getDefault(),
                    RestClientMessage.class.getClassLoader());
            if (bundle == null)
            {
                return key;
            }

            String message = bundle.getString(key);
            // format the message based on the parameters
            MessageFormat formatter = new MessageFormat(message);
            return formatter.format(params);
        }
        catch (MissingResourceException e)
        {
            // if a message is not found, return the key back
            log.debug("Key " + key + " is not found in message bundle." );
            return  key;
        }
    }

    /**
     * Gets the internationalized message based on the value of the key and the
     * appropriate <code>ResourceBundle</code> for the desired
     * <code>Locale</code>. The message is formatted using a
     * <code>MessageFormat</code> object to substitute the supplied set of
     * values into the message.
     *
     * @param locale
     * @param pKey
     *            - message key
     * @param pArgs
     *            - message arguments
     * @return    - message in the appropriated locale, or message key if the
     *         message doesn't exist in the resource bundle
     */
    public static String getMessage(Locale locale, String key, Object... params)
    {
        try
        {
            ResourceBundle bundle = loadResourceBundle(defaultBundleName, locale,
                    RestClientMessage.class.getClassLoader());
            if (bundle == null)
            {
                return key;
            }

            String message = bundle.getString(key);
            // format the message based on the parameters
            MessageFormat formatter = new MessageFormat(message);
            return formatter.format(params);
        }
        catch (MissingResourceException e)
        {
            // if a message is not found, return the key back
            log.debug("Key " + key + " is not found in message bundle." );
            return  key;
        }
    }


    /**
     * load a message bundle of the specified locale if it has not been
     * loaded yet. If message bundle of the specified locale is not found,
     * it returns the bundle of the default locale.
     *
     * @param bundleName
     * @param locale
     * @param classLoader
     */
    private static ResourceBundle loadResourceBundle(String bundleName,
            Locale locale, ClassLoader classLoader)
    {
        // get the bundle of the specified locale from cache
        ResourceBundle bundle = bundleCache.get(locale);
        if (bundle == null)
        {
            try
            {
                // load the bundle of the specified locale
                bundle = ResourceBundle.getBundle(bundleName, locale, classLoader);
                log.debug("Successfully loaded message bundle " + bundleName + " of locale "
                        + locale.getDisplayName());
                bundleCache.put(locale, bundle);
            }
            catch (MissingResourceException e)
            {
                // unable to load the bundle of the specified locale.
                // now, get the bundle of default locale from cache
                log.debug("Unable to load message bundle " + bundleName + " of locale "
                        + locale.getDisplayName());
                log.debug("Try to load message bundle " + bundleName + " with default locale "
                        + locale.getDisplayName());
                bundle = bundleCache.get(Locale.getDefault());
                if (bundle == null)
                {
                    try
                    {
                        // bundle of the default locale is not in the cache,
                        // load the default locale bundle, and store it in cache
                        bundle = ResourceBundle.getBundle(bundleName,
                                Locale.getDefault(), classLoader);
                        bundleCache.put(locale, bundle);
                    }
                    catch (MissingResourceException e1)
                    {
                        // This should not happen.
                        log.debug("Failed to load message bundle " + bundleName + " of locale "
                                + Locale.getDefault());
                    }
                }
            }
        }

        return bundle;
    }
}
