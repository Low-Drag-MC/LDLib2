package com.lowdragmc.lowdraglib2.networking.rpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With {@code @RPCPacket}, you can declare a static method ANYWHERE in your codebase and treat it as a network packet handler.
 * The annotated method itself becomes the packet’s execution target, and its parameters represent the data transferred between client and server.
 *
 * <li> {@code @RPCPacket(id)}: Registers the method as an RPC handler with a unique identifier.</li>
 *
 * <li> Method parameters: All parameters (except RPCSender) are automatically serialized and transferred.</li>
 *
 * <li> `RPCSender` (optional): If declared as the first parameter, LDLib2 injects sender-side information, allowing you to distinguish whether the call is executed on the client or the server.</li>
 *
 * RPCPacketDistributor
 * Provides utility methods to send RPC calls to the server, all players, or specific targets.
 * <pre>{@code
 * // annotate your packet method anywhere you want
 * @RPCPacket("rpcPacketTest")
 * public static void rpcPacketTest(RPCSender sender, String message, boolean message2) {
 *     if (sender.isServer()) {
 *         LDLib2.LOGGER.info("Received RPC packet from server: {}, {}", message, message2);
 *     } else {
 *         LDLib2.LOGGER.info("Received RPC packet from client: {}, {}", message, message2);
 *     }
 * }
 *
 * // send pacet to the remote/server
 * RPCPacketDistributor.rpcToServer("rpcPacketTest", "Hello from client!", true)
 * RPCPacketDistributor.rpcToAllPlayers("rpcPacketTest", "Hello from server!", false)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RPCPacket {
    String value();
    /**
     * if the value is non-empty, only register the packet if and if the mod is loaded.
     */
    String modId() default "";
}
