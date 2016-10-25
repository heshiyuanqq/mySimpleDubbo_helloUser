package demo1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import demo.demo1.HelloService;
import demo.demo1.User;
import demo.demo1.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class HelloServiceTest {

    @Autowired
    private RpcProxy rpcProxy;

    @Test
    public void helloTest() {
        HelloService helloService = rpcProxy.create(HelloService.class);
        String result = helloService.hello("World");
        System.out.println("result="+result+"^^^^^^^^^^^^^^^^");
       // Assert.assertEquals("Hello! World", result);
    }
    
    
    @SuppressWarnings("resource")
	public static void main(String[] args) {
				ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
				RpcProxy proxy = (RpcProxy) context.getBean("rpcProxy");
				UserService userService = proxy.create(UserService.class);
				User user=new User();
				user.setAddress("北京市西城区");
				user.setAge(23);
				user.setGender(1);
				user.setHobbys(new String[]{"骑马","射箭","潜水"});
				user.setName("张三丰");
				userService.addUser(user);
				userService.deleteUser(user);
				/*String result = helloService.hello("张三三！");
				 System.out.println("result="+result+"***********************************");
				*/
				
	}
}