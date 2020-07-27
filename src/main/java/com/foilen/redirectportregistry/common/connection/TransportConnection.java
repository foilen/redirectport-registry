/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.connection;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.redirectportregistry.RedirectionPortRegistryException;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.smalltools.tools.CloseableTools;

/**
 * This is an association from a raw service to a connection id.
 */
public class TransportConnection implements Closeable, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TransportConnection.class);

    private static final int BUFFER_SIZE = 100 * 1024; // 100 KB

    private String connectionId;
    private Socket socket;

    // Cached
    private InputStream inputStream;
    private OutputStream outputStream;

    // Services
    private MessageSenderService messageSenderService;
    private Thread thread;

    // State
    private boolean localDisconnected = false;
    private boolean remoteDisconnected = false;
    private AtomicBoolean dataProcessedSinceLastCheck = new AtomicBoolean(true);

    // Sending queue
    private ExecutorService sendingDataExecutorService = Executors.newSingleThreadExecutor();

    public TransportConnection(String connectionId, Socket socket, MessageSenderService messageSenderService) {
        this.connectionId = connectionId;
        this.socket = socket;
        this.messageSenderService = messageSenderService;
    }

    /**
     * Tells the raw socket to close once all the queued messages are sent. Is non-blocking.
     */
    @Override
    public void close() {
        sendingDataExecutorService.submit(() -> {
            CloseableTools.close(socket);
            localDisconnected = true;
            remoteDisconnected = true;
        });
    }

    /**
     * Check if data was retrieved or sent since the last time that method was called.
     *
     * @return true if data was retrieved or sent
     */
    public boolean dataProcessedSinceLastCheck() {
        return dataProcessedSinceLastCheck.getAndSet(false);
    }

    public void forceSocketClose() {
        CloseableTools.close(inputStream);
        CloseableTools.close(outputStream);
        CloseableTools.close(socket);
        localDisconnected = true;
        remoteDisconnected = true;
    }

    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Tells the raw socket's output stream to close once all the queued messages are sent. Is non-blocking.
     */
    public void gotRemoteDisconnection() {
        sendingDataExecutorService.submit(() -> {
            CloseableTools.close(outputStream);
            remoteDisconnected = true;
        });
    }

    public void init(String connectionId) {

        this.connectionId = connectionId;

        // Get the streams
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            throw new RedirectionPortRegistryException("Could not take the streams of the socket", e);
        }

        // Start the reading
        thread = new Thread(this, "TransportConnection-" + connectionId);
        thread.start();
    }

    public boolean isCompletelyDisconnected() {
        return localDisconnected && remoteDisconnected;
    }

    public boolean isPartiallyOrCompletelyDisconnected() {
        return localDisconnected || remoteDisconnected;
    }

    /**
     * Read all the data that comes in and send it over the bridge.
     */
    @Override
    public void run() {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while (true) {
                // Wait and read 1 byte
                len = inputStream.read(buffer, 0, 1);
                if (len == 0) {
                    continue;
                }
                if (len == -1) {
                    break;
                }

                dataProcessedSinceLastCheck.set(true);

                // Read as much as possible
                len = inputStream.read(buffer, 1, BUFFER_SIZE - 1);
                if (len == -1) {
                    messageSenderService.sendData(connectionId, buffer, 1);
                    break;
                }
                messageSenderService.sendData(connectionId, buffer, len + 1);
            }
        } catch (Exception e) {
            // Close
        } finally {
            localDisconnected = true;
            try {
                messageSenderService.sendDisconnect(connectionId);
            } catch (Exception e) {
                logger.info("Tried to disconnect an already disconnected connection {}", connectionId);
            }
        }
    }

    /**
     * Write data to the raw socket. Is non-blocking and queuing the data.
     *
     * @param data
     *            the data to write
     */
    public void writeData(byte[] data) {
        sendingDataExecutorService.submit(() -> {
            try {
                dataProcessedSinceLastCheck.set(true);
                outputStream.write(data);
            } catch (Exception e) {
                logger.info("Writing data to connection {} failed. Closing the socket", connectionId);
                CloseableTools.close(socket);
            }
        });
    }

}
