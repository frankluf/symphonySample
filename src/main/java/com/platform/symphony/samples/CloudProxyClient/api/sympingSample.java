/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * 5725-G86
 * @ (C) Copyright IBM Corporation 2001, 2020, All Rights Reserved
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.platform.symphony.samples.CloudProxyClient.api;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.platform.symphony.samples.CloudProxyClient.api.SymObj.Connection;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.Message;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.Session;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.SessionCreationAttributes;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.SoamFactory;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.TaskInputHandle;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.TaskOutputFormat;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.TaskOutputHandle;
import com.platform.symphony.samples.CloudProxyClient.api.SymObj.TaskSubmissionAttributes;

/**
 * symping test client
 *
 */
public class sympingSample {
    public static final String APPLICATION_NANE = "symping7.3.1";
    public static final int SUBMIT_TASK_COUNT = 10;
    public static final int FETCH_TASK_COUNT = 10;
    public static final long WAIT_FOR_TASK_EXECUTION = 7000L;
    public static final long WAIT_FOR_SESSION_CREATE = 2000L;
    public static final int RETRY_TIME = 4;

    public sympingSample() {
        SoamFactory.initialize();
    }

    public void run() {
        int retryTime = 0;
        // if failed in sending task, it will retry 3 time.
        while (retryTime < RETRY_TIME) {
            if (retryTime != 0) {
                System.err.println();
                System.err.println();
                System.err.println("-----------------Start to retry-------------------------------");
            }
            if (exuectue()) {
                break;
            }
            retryTime++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }

    private boolean exuectue() {
        Connection conn = null;
        Session session = null;
        try {
            conn = SoamFactory.connect("symping7.3.1");

            SessionCreationAttributes attributes = new SessionCreationAttributes();
            attributes.setSessionName("MySession");
            attributes.setSessionType("UnrecoverableNoHistoricalData");

            session = conn.createSession(attributes);

            // create session failed
            if (session == null) {
                throw new Exception("Create session failed. Please see the log files for details.");
            }

            System.out.println("------------------------------------------------------------");
            System.out.println("Creating 1 session...");
            System.out.println("Session created. ID: " + StringUtils.defaultIfBlank(session.getSessionId(), ""));
            System.out.println("Application        : " + StringUtils.defaultIfBlank(session.getApplicationName(), ""));
            System.out.println();
            System.out.println();

            // wait for task execution.
            try {
                Thread.sleep(WAIT_FOR_SESSION_CREATE);
            } catch (InterruptedException e) {
            }

            List<TaskSubmissionAttributes> attrList = new LinkedList<TaskSubmissionAttributes>();
            for (int i = 0; i < SUBMIT_TASK_COUNT; i++) {
                TaskSubmissionAttributes taskAttr = new TaskSubmissionAttributes();
                taskAttr.setTaskTag("myTag" + i);
                //taskAttr.setTaskPriority(10);

                MyInput input = new MyInput(5, 0, 0, 0, false, 50000L, new Byte[0]);
                taskAttr.setTaskInput(input.getData());
                attrList.add(taskAttr);
            }
            TaskSubmissionAttributes realAttrList = new TaskSubmissionAttributes();
            realAttrList.setTaskAttrlist(attrList);
            session.addTaskInput(realAttrList);

            System.out.println("------------------------------------------------------------");
            System.out.println("Starting to submit task...");
            System.out.println("Sending " + SUBMIT_TASK_COUNT + " tasks and retrieving replies...");
            TaskInputHandle taskInputHandleList = session.sendTaskInput();
            for (int i = 0; i < taskInputHandleList.getTaskIds().size(); i++) {
                TaskInputHandle taskInputHandle = taskInputHandleList.getTaskIds().get(i);
                if (StringUtils.isBlank(taskInputHandle.getErrorMessage())) {
                    System.out.println("Task " + StringUtils.defaultIfBlank(taskInputHandle.getTaskId(), "")
                            + " submitted successfully.");
                } else {
                    System.out.println("Task " + StringUtils.defaultIfBlank(taskInputHandle.getTaskId(), "")
                            + " submitted failed. Error message is: " + taskInputHandle.getErrorMessage());
                }
            }
            System.out.println();
            System.out.println();
            // wait for task execution.
            try {
                Thread.sleep(WAIT_FOR_TASK_EXECUTION);
            } catch (InterruptedException e) {
            }

            TaskOutputFormat format = new TaskOutputFormat();
            format.addFormat("SOAM_INT32");
            format.addFormat("SOAM_INT32");
            format.addFormat("SOAM_DOUBLE");
            format.addFormat("SOAM_DOUBLE");
            format.addFormat("SOAM_STRING_CHARS");
            format.addFormat("SOAM_UINT64");
            format.addFormat("SOAM_INT32");
            format.addFormat("SOAM_UINT64");

            System.out.println("------------------------------------------------------------");
            System.out.println("Fetching " + FETCH_TASK_COUNT + " task results...");
            TaskOutputHandle outputHandleList = session.fetchTaskOutput(FETCH_TASK_COUNT, format);

            for (TaskOutputHandle handle : outputHandleList.getOutputList()) {
                if (handle.isSuccsss()) {
                    System.out.println("taskId: " + StringUtils.defaultIfBlank(handle.getTaskId(), ""));
                    System.out.println("taskTag: " + StringUtils.defaultIfBlank(handle.getTaskTag(), ""));
                    System.out.println(
                            "lastTaskRecoveryId: " + StringUtils.defaultIfBlank(handle.getLastTaskRecoveryId(), ""));
                    System.out
                            .println("outputData: " + (handle.getOutputData() == null ? "[]" : handle.getOutputData()));
                } else {
                    System.out.println("errorMessage: " + StringUtils.defaultIfBlank(handle.getErrorMessage(), ""));
                }

                System.out.println();
            }
            System.out.println();
            System.out.println();

            System.out.println("------------------------------------------------------------");
            System.out.println("Closing Session...");
            session.closeSession();
            System.out.println("Session " + session.getSessionId() + " is closed");

        } catch (Exception ex) {
            ex.printStackTrace();
            if (null != session) {
                session.closeSession();
                System.out.println("Session " + session.getSessionId() + " is closed");
            }
            return false;
        } finally {
            SoamFactory.uninitialize();
        }
        return true;
    }

    class MyInput extends Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Integer in_size;
        private Integer o_size;
        private Integer runtime;
        private Boolean realcpu;
        private Long svclogopt;
        private Byte[] binary;

        public MyInput(Integer id, Integer in_size, Integer o_size, Integer runtime, Boolean realcpu, Long svclogopt,
                Byte[] binary) {
            this.id = id;
            this.in_size = in_size;
            this.o_size = o_size;
            this.runtime = runtime;
            this.realcpu = realcpu;
            this.svclogopt = svclogopt;
            this.realcpu = realcpu;
            this.binary = binary;
        }

        public List<String[]> getData() {
            List<String[]> list = new LinkedList<String[]>();
            list.add(this.onSerialize(this.id));
            list.add(this.onSerialize(this.in_size));
            list.add(this.onSerialize(this.o_size));
            list.add(this.onSerialize(this.runtime));
            list.add(this.onSerialize(this.realcpu));
            list.add(this.onSerialize(this.svclogopt));
            list.add(this.onSerialize(this.binary));
            return list;
        }

    }
}
