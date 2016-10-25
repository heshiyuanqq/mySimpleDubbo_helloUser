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
	        return (T) Proxy.newProxyInstance(
		            interfaceClass.getClassLoader(),
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