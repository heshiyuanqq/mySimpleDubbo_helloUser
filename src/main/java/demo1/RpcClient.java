package demo1;

import java.util.concurrent.CountDownLatch;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import demo.demo1.RpcDecoder;
import demo.demo1.RpcEncoder;
import demo.demo1.RpcRequest;
import demo.demo1.RpcResponse;


/**
 * 使用RpcClient类实现 RPC 客户端，只需扩展 Netty 提供的SimpleChannelInboundHandler抽象类即可
 * @author Administrator
 *
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);
    private String host;
    private int port;
    private RpcResponse response;
    private final Object obj = new Object();
    private CountDownLatch latch=new CountDownLatch(2);

    public RpcClient(String host, int port) {
	        this.host = host;
	        this.port = port;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
	        this.response = response;
	      /*  synchronized (obj) {
	            	obj.notifyAll(); // 收到响应，唤醒线程
	        }*/
	        latch.countDown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }

    public RpcResponse send(RpcRequest request) throws Exception {
		        EventLoopGroup group = new NioEventLoopGroup();
		        try {
			            Bootstrap bootstrap = new Bootstrap();
			            bootstrap.group(group).channel(NioSocketChannel.class)
				                 .handler(new ChannelInitializer<SocketChannel>() {
						                    @Override
						                    public void initChannel(SocketChannel channel) throws Exception {
						                        	 channel.pipeline()
								                            .addLast(new RpcEncoder(RpcRequest.class)) // 将 RPC 请求进行编码（为了发送请求）
								                            .addLast(new RpcDecoder(RpcResponse.class)) // 将 RPC 响应进行解码（为了处理响应）
								                            .addLast(RpcClient.this/*内部监听调用channelRead0*/); // 使用 RpcClient 发送 RPC 请求
						                    }
				                 })
				                 .option(ChannelOption.SO_KEEPALIVE, true);
			
			            /**
			             * 使用了obj的wait和notifyAll来等待Response返回，会出
	                     * 现“假死等待”的情况：一个Request发送出去后，在obj.wait()调用之前可能Response就返回了，
	                     * 这时候在channelRead0里已经拿到了Response并且obj.notifyAll()已经在obj.wait()之前调用了，
	                     * 这时候send后再obj.wait()就出现了假死等待，客户端就一直等待在这里。使用CountDownLatch
	                     * 可以解决这个问题。
			             */
			            ChannelFuture future = bootstrap.connect(host, port).sync();
			            future.channel().writeAndFlush(request).sync();
			/*
			            synchronized (obj) {
			                	obj.wait(); // 未收到响应，使线程等待
			            }
			            */
			            latch.await();
			
			            
			           
			            
			            if (response != null) {
			                	future.channel().closeFuture().sync();
			            }
			            return response;
		        } finally {
		            	group.shutdownGracefully();
		        }
    }
}