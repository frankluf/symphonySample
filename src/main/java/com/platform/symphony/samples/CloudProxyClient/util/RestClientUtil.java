/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2016, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.platform.symphony.samples.CloudProxyClient.api.SymAPIExecutor;
import com.platform.symphony.samples.CloudProxyClient.constant.RestClientConstant;
import com.platform.symphony.samples.CloudProxyClient.rest.RESTRequestExecutor;

/**
 * Utlitiy class
 *
 *
 */
public final class RestClientUtil {
    private static Logger log = Logger.getLogger(RestClientUtil.class);

    private RestClientUtil() {

    }

    public static boolean success(Map<String, String> result) {
        if (StringUtils.isNotBlank(result.get("status"))) {
            String status = result.get("status");
            if (status.equals("200") || status.equals("401") || status.equals("403")) {
                return true;
            }
        }
        return false;
    }

    public static void initLogger() {
        String log4jConfDir = "";
        if (StringUtils.isBlank(log4jConfDir) && StringUtils.isNotBlank(System.getProperty("CONF_DIR"))) {
            log4jConfDir = System.getProperty("CONF_DIR");
        }
        if (StringUtils.isNotBlank(log4jConfDir)) {
            try {
                FileInputStream log4jStream = new FileInputStream(log4jConfDir+File.separator+"symrest_client.log4j.properties");
                Properties props = new Properties();
                props.load(log4jStream);
                PropertyConfigurator.configure(props);
            } catch (IOException e) {
                System.out.println(("Cannot read configuration file symrest_client.json. Check for JSON format errors and try again. Exiting the program..."));
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.out.println(("Cannot read configuration file symrest_client.json. Check for JSON format errors and try again. Exiting the program..."));
            System.exit(1);
        }
    }

    /**
     * Clean up all resource when program exit.
     */
    public static void clean() {
        RESTRequestExecutor.getInstance().clean();
    }

    /**
     * Transform json File to Object
     *
     * @param jsonFile
     * @param type
     * @return
     */
    public static <T> T toObject(File jsonFile, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonFile, type);
        } catch (IOException e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.changeJSONFileToObjectError"), e);
        }
        return null;
    }

    /**
     * Transform json String to Object
     *
     * @param <T>
     * @param jsonFile
     * @param typeReference
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T toObject(File jsonFile, TypeReference<T> typeReference) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (T) mapper.readValue(jsonFile, typeReference);
        } catch (IOException e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.changeJSONTextToObjectError"), e);
        }
        return null;
    }

    /**
     * Transform json String to Object
     *
     * @param jsonTxt
     * @param type
     * @return
     */
    public static <T> T toObject(String jsonTxt, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonTxt, type);
        } catch (IOException e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.changeJSONTextToObjectError"), e);
        }
        return null;
    }

    /**
     * Transform json String to Object
     *
     * @param <T>
     * @param jsonFile
     * @param typeReference
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T toObject(String jsonTxt, TypeReference<T> typeReference) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (T) mapper.readValue(jsonTxt, typeReference);
        } catch (IOException e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.changeJSONTextToObjectError"), e);
        }
        return null;
    }


    public static <T> String toJson(T t) {
        String result = "";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            result =  mapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.changeJSONTextToObjectError"), e);
        }
        return result;
    }

    public static String prettyJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(json, Object.class));
            return indented;
        } catch (Exception e) {
        }
        return json;
    }



    public static String readJsonFromFile(String filePath) {
        StringBuilder sb = new StringBuilder(300);
        Scanner reader = null;
        try {
            reader = new Scanner(new FileReader(filePath));
            while (reader.hasNext()) {
                sb.append(reader.nextLine());
            }
        } catch (FileNotFoundException e) {}
        finally {
            if (reader != null)
                reader.close();
        }
        return sb.toString();
    }

    /**
     * Parase command line argument
     * @param args
     * @throws Exception
     */
    public static Map<String, String> parseArgs(String[] args) {
        initLogger();
        Options opts = new Options();
        opts.addOption("h", false, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.help"));
        opts.addOption("appName", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.application"));
        opts.addOption("action", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.action"));
        opts.addOption("sessionId", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.sessionId"));
        opts.addOption("clusterId", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.clusterId"));
        opts.addOption("source", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.source"));
        opts.addOption("count", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.count"));
        opts.addOption("filter", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.filter"));
        opts.addOption("closeFlag", true, RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.closeFlag"));

        BasicParser parser = new BasicParser();
        CommandLine cl;
        Map<String, String> result = new HashMap<String, String>();
        try {
            cl = parser.parse(opts, args);
            if (cl.getOptions().length > 0) {
                if (cl.hasOption('h')) {
                    HelpFormatter hf = new HelpFormatter();
                    hf.printHelp(" ", opts);
                    System.exit(1);
                } else {
                    result.put(RestClientConstant.ACTION_PARAM_NAME, StringUtils.defaultIfBlank(cl.getOptionValue("action"), ""));
                    result.put(RestClientConstant.APP_NAME, StringUtils.defaultIfBlank(cl.getOptionValue("appName"), ""));
                    result.put(RestClientConstant.SESSION_ID, StringUtils.defaultIfBlank(cl.getOptionValue("sessionId"), ""));
                    result.put(RestClientConstant.CLUSTER_ID, StringUtils.defaultIfBlank(cl.getOptionValue("clusterId"), ""));
                    result.put(RestClientConstant.PARAMTER_JSON, StringUtils.defaultIfBlank(cl.getOptionValue("source"), ""));
                    result.put(RestClientConstant.COUNT_MAX, StringUtils.defaultIfBlank(cl.getOptionValue("count"), ""));
                    result.put(RestClientConstant.FILTER, StringUtils.defaultIfBlank(cl.getOptionValue("filter"), ""));
                    result.put(RestClientConstant.CLOSE_FLAG, StringUtils.defaultIfBlank(cl.getOptionValue("closeFlag"), ""));
                }
            }
        } catch (Exception e) {
            log.error(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.commandParseError"), e);
            System.out.println(RestClientMessage.getMessage("com.ibm.spectrum.RestClientUtil.commandParseError"));
            System.exit(1);
        }
        return result;
    }

    /**
     * invoke seperate HTTP REST request for create session/submit task/fetch result/close session
     * @param input HTTP Parameter
     * @return
     */
    public static Map<String, String> invokeSymAPI(Map<String, String> input) {
        Map<String, String> result;
        try {
            // init logger
            initLogger();

            // common parameter container
            Map<String, Object> httpRequestParam = new HashMap<String, Object>();
            httpRequestParam.putAll(input);


            //  headers for http request
            Map<String, String> header = new HashMap<String, String>();
            header.put("Accpet", RestClientConstant.JSON_TYPE);
            header.put("Content-Type", RestClientConstant.JSON_TYPE);

            // convert -source argument to json
            if (StringUtils.isNotBlank(input.get(RestClientConstant.PARAMTER_JSON))) {
                if (input.get(RestClientConstant.PARAMTER_JSON).startsWith("{") && input.get(RestClientConstant.PARAMTER_JSON).endsWith("}")) {

                } else { // It's a json file path.
                    httpRequestParam.put(RestClientConstant.PARAMTER_JSON, readJsonFromFile(input.get(RestClientConstant.PARAMTER_JSON)));
                }
            }

            SymAPIExecutor executor = new SymAPIExecutor();
            result = executor.execute(httpRequestParam, header);
        } finally {
            // Clean resource
            clean();
        }
        return result;
    }
}
