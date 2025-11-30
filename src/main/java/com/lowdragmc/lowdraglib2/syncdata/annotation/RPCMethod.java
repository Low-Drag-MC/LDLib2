package com.lowdragmc.lowdraglib2.syncdata.annotation;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.lowdragmc.lowdraglib2.syncdata.holder.IRPCManagedHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Annotate a method, you can send RPC packet between server and remote. You are free to define the parameters of the methods long as the parameters support sync, and send rpc anywhere in your class.
 * It is useful to spread an event ({@code c->s} / {@code s->c}).
 * <br>
 * Make sure that all args match the parameters of annotated method.
 * <pre>{@code
 * @RPCMethod
 * public void rpcTestA(RPCSender sender, String message) {
 *     if (sender.isServer()) {
 *         LDLib2.LOGGER.info("Received RPC from server: {}", message);
 *     } else {
 *         LDLib2.LOGGER.info("Received RPC from client: {}", message);
 *     }
 * }
 *
 * @RPCMethod
 * public void rpcTestB(ItemStack item) {
 *     LDLib2.LOGGER.info("Received RPC: {}", item);
 * }
 *
 * // methods to send rpc
 * public void sendMsgToPlayer(ServerPlayer player, String msg) {
 *     rpcToServer(player, "rpcTestA", msg)
 * }
 *
 * public void sendMsgToAllTrackingPlayers(ServerPlayer player, String msg) {
 *     rpcToTracking("rpcTestA", msg)
 * }
 *
 * public void sendMsgToServer(ItemStack item) {
 *     rpcToServer("rpcTestB", item)
 * }
 * }</code></pre>
 *
 * <li> {@link IRPCManagedHolder#rpcToTracking(IManaged, String, Object...)}: send to all remote players if this chunk is loaded(tracked) in their remotes. </li>
 * <li> {@link IRPCManagedHolder#rpcToPlayer(IManaged, ServerPlayer, String, Object...)}: send to a specfic player </li>
 * <li> {@link IRPCManagedHolder#rpcToServer(IManaged, String, Object...)}: send to server. </li>
 *
 * <pre>{@code
 * @RPCMethod
 * public void rpcTest(String msg) {
 *     if (level.isClient) { // receive
 *         LDLib2.LOGGER.info("Received RPC from server: {}", message);
 *     } else { // send
 *         rpcToTracking("rpcTest", msg)
 *     }
 * }
 * }</pre>
 * In this example, you can send and receive msg within one method, which is a neat method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RPCMethod {

}
