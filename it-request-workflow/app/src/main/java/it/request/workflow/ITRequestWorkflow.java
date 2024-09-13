package it.request.workflow;

import io.littlehorse.sdk.common.proto.Comparator;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.VariableMutationType;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.wfsdk.LHFormatString;
import io.littlehorse.sdk.wfsdk.UserTaskOutput;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.wfsdk.WorkflowThread;

public class ITRequestWorkflow {

    private LittleHorseBlockingStub client;

    public ITRequestWorkflow(LittleHorseBlockingStub client) {
        this.client = client;
    }

    public void registerWfSpec() {
        Workflow workflow = Workflow.newWorkflow("it-request", this::wfLogic);
        workflow.registerWfSpec(client);
    }

    private void wfLogic(WorkflowThread wf) {
        WfRunVariable email =
                wf.addVariable("email", VariableType.STR).required().searchable();
        WfRunVariable item = wf.addVariable("item", VariableType.STR).required();
        WfRunVariable justification =
                wf.addVariable("justification", VariableType.STR).required();

        WfRunVariable status = wf.addVariable("status", "PENDING").searchable();
        WfRunVariable isApproved = wf.addVariable("is-approved", VariableType.BOOL);
        WfRunVariable comments = wf.addVariable("comments", VariableType.STR);

        String userId = null;
        String userGroup = "finance";
        UserTaskOutput approvalOutput = wf.assignUserTask("approve-it-request", userId, userGroup)
                .withNotes(
                        wf.format("User {0} is requesting item {1}. Justification: {2}", email, item, justification));

        wf.mutate(isApproved, VariableMutationType.ASSIGN, approvalOutput.jsonPath("$.isApproved"));
        wf.mutate(comments, VariableMutationType.ASSIGN, approvalOutput.jsonPath("$.comments"));

        wf.doIfElse(
                wf.condition(isApproved, Comparator.EQUALS, true),
                ifBody -> {
                    wf.mutate(status, VariableMutationType.ASSIGN, "APPROVED");
                    LHFormatString emailBody = ifBody.format("You request for {0} was approved!", item);
                    ifBody.execute("send-email", email, "IT Request Approved", emailBody);
                },
                elseBody -> {
                    wf.mutate(status, VariableMutationType.ASSIGN, "REJECTED");
                    LHFormatString emailBody =
                            elseBody.format("You request for {0} was rejected because {1}!", item, comments);
                    elseBody.execute("send-email", email, "IT Request Rejected", emailBody);
                });
    }
}
