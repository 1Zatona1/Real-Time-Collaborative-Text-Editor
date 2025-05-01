//package com.example.apt_project;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.BiConsumer;
//import java.util.function.Consumer;
//
//public class SessionManager {
//    private String userId;
//    private BiConsumer<Operation, List<String>> applyRemoteOperation;
//    private Consumer<List<String>> updateUserPresence;
//    private TextEditorWebsokcet websocket;
//    private List<List<Operation>> operationBuffer;
//
//    public SessionManager(String userId, BiConsumer<Operation, List<String>> applyRemoteOperation, Consumer<List<String>> updateUserPresence) {
//        this.userId = userId;
//        this.applyRemoteOperation = applyRemoteOperation;
//        this.updateUserPresence = updateUserPresence;
//        this.operationBuffer = new ArrayList<>();
//    }
//
//    public void setWebSocket(TextEditorWebsokcet websocket) {
//        this.websocket = websocket;
//        if (!operationBuffer.isEmpty() && websocket.isOpen()) {
//            operationBuffer.forEach(ops -> ops.forEach(op -> websocket.send(op.toString())));
//            operationBuffer.clear();
//        }
//    }
//
//    public void applyRemoteOperation(Operation operation, List<String> users) {
//        applyRemoteOperation.accept(operation, users);
//    }
//
//    public void updateUserPresence(List<String> users) {
//        updateUserPresence.accept(users);
//    }
//
//    public void bufferOperation(List<Operation> operations) {
//        operationBuffer.add(operations);
//    }
//}