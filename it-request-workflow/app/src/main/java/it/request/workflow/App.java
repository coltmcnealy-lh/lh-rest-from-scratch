/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package it.request.workflow;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.PutUserTaskDefRequest;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.usertask.UserTaskSchema;
import io.littlehorse.sdk.usertask.annotations.UserTaskField;

/*
1. Workflow starts when a user/employee submits a request for IT
  - item
  - justification
2. Finance team approves / rejects the request (User Task)
3. Notify the user / employee whether their request was accepted.

- UserTaskDef: approve-it-request
- TaskDef: send-email
 */

class ITRequestApprovalForm {
    @UserTaskField(required = false, description = "Why did you reject/approve it?")
    public String comments;

    @UserTaskField(required = true, description = "True if request is approved")
    public boolean isApproved;
}


public class App {
    public static void main(String[] args) {
        LHConfig config = new LHConfig();
        LittleHorseBlockingStub client = config.getBlockingStub();

        UserTaskSchema schema = new UserTaskSchema(new ITRequestApprovalForm(), "approve-it-request");
        PutUserTaskDefRequest putUserTaskDefRequest = schema.compile();
        client.putUserTaskDef(putUserTaskDefRequest);

        ITRequestWorkflow workflowGenerator = new ITRequestWorkflow(client);
        workflowGenerator.registerWfSpec();
    }
}
