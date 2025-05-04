package com.stark.server;

import com.stark.server.Operation;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SessionService {
    private Map<Integer, List<String>> sessions = new ConcurrentHashMap<>();
    AtomicInteger newSessionId=new AtomicInteger(1);

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        String editorCode = "E-" + UUID.randomUUID().toString().substring(0, 8);
        String viewerCode = "V-" + UUID.randomUUID().toString().substring(0, 8);
        sessions.put(sessionId, new List<String>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<String> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(String s) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public String get(int index) {
                return "";
            }

            @Override
            public String set(int index, String element) {
                return "";
            }

            @Override
            public void add(int index, String element) {

            }

            @Override
            public String remove(int index) {
                return "";
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<String> listIterator() {
                return null;
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return null;
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return List.of();
            }
        });
        sessionRoles.put(editorCode, sessionId);
        sessionRoles.put(viewerCode, sessionId);
        return editorCode + "," + viewerCode + "," + sessionId;
    }

    public boolean addUser(String sessionId, WebSocketSession session, String role) {
        Map<String, WebSocketSession> sessionUsers = sessions.getOrDefault(sessionId, new ConcurrentHashMap<>());
        if (sessionUsers.size() >= 4) {
            return false;
        }
        sessionUsers.put(session.getId(), session);
        sessions.put(sessionId, sessionUsers);
        return true;
    }

    public void removeUser(String sessionId, WebSocketSession session) {
        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            sessionUsers.remove(session.getId());
            if (sessionUsers.isEmpty()) {
                sessions.remove(sessionId);
            }
        }
    }

    public void broadcastOperation(String sessionId, Operation operation) {
        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            String json = gson.toJson(operation);
            for (WebSocketSession userSession : sessionUsers.values()) {
                try {
                    if (userSession.isOpen()) {
                        userSession.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    System.out.println("Error broadcasting to " + userSession.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public String validateSessionCode(String code) {
        return sessionRoles.getOrDefault(code, null);
    }
}