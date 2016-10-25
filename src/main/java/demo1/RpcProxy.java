package demo1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import demo.demo1.RpcRequest;
import demo.demo1.RpcResponse;
/**
 * 使用 Java 提供的动态代理技术实现 RPC 代理（当然也可以使用 CGLib 来实现）
 * @author Administrator
 *
 */
public class RpcProxy {

    private String serverAddress;
    private ServiceDiscovery serviceDiscovery;

    public RpcProxy(String serverAddress) {
        	this.serverAddress = serverAddress;
    }

    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        	this.serviceDiscovery = serviceDiscovery;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> interfaceClass) {
	          return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
									            new Class<?>[]{interfaceClass},
									            new InvocationHandler() {
										                @Override
										                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
											                    RpcRequest request = new RpcRequest(); // 创建并初始化 RPC 请求
											                    request.setRequestId(UUID.randomUUID().toString());
											                    request.setClassName(method.getDeclaringClass().getName());
											                    request.setMethodName(method.getName());
											                    request.setParameterTypes(method.getParameterTypes());
											                    request.setParameters(args);
											
											                    if (serviceDiscovery != null) {
											                        	serverAddress = serviceDiscovery.discover(); // 仅仅是这里利用zookeeper发现服务
											                        	System.out.println("serverAddress="+serverAddress);
											                    }
											
											                    String[] array = serverAddress.split(":");
											                    String host = array[0];
											                    int port = Integer.parseInt(array[1]);
											                    //以上利用zookeeper获取服务的主机和端口
											                    //已知端口利用netty封装的tcp(nio协议)远程调用host:port中的服务
											                    
											                    
											                    /**
											                     * 这里每次使用代理远程调用服务，从Zookeeper上获取可用的服务地址，通过RpcClient send
											                     * 一个Request，等待该Request的Response返回。这里原文有个比较严重的bug，在原文给出的
											                     * 简单的Test中是很难测出来的，原文使用了obj的wait和notifyAll来等待Response返回，会出
											                     * 现“假死等待”的情况：一个Request发送出去后，在obj.wait()调用之前可能Response就返回了，
											                     * 这时候在channelRead0里已经拿到了Response并且obj.notifyAll()已经在obj.wait()之前调用了，
											                     * 这时候send后再obj.wait()就出现了假死等待，客户端就一直等待在这里。使用CountDownLatch
											                     * 可以解决这个问题。注意：这里每次调用的send时候才去和服务端建立连接，使用的是短连接，
											                     * 这种短连接在高并发时会有连接数问题，也会影响性能。
											                     */
											                    
											                    
											                    RpcClient client = new RpcClient(host, port); // 初始化 RPC 客户端
											                    //这里用的是netty框架用nio技术封装的tcp协议（比web service的http协议高效，因为http是更高层的协议(应用层)，而tcp是传输层协议）
											                    RpcResponse response = client.send(request); // 通过 RPC 客户端发送 RPC 请求并获取 RPC 响应
											
											                    if (response.getError()!=null) {
											                    		throw response.getError();
											                    } else {
											                        	return response.getResult();
											                    }
										                }
									            }
	        );
    }
}