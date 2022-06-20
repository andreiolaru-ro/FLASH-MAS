package gabrielvoicu.httpDeployment;

import net.xqhs.flash.FlashBoot;

public class BootSimpleHttpsDeployment
{
	public static void main(String[] args_)
	{
		String args = "";

		args += " -package example.simplePingPong";

		args += " -node node1";
		args += " -pylon http:slave1 serverPort:8885 https:true cert:/gabrielvoicu/httpDeployment/server_private_cert.jks connectTo:https://localhost:8886/AgentB"
			+ " trustedCa:/gabrielvoicu/httpDeployment/ca.pem";
		args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		args += " -agent AgentC classpath:AgentPingPong sendTo:AgentB";

		//		http://localhost:8885/AgentA
		//		http://localhost:8885/AgentC

		args += " -node node2";
		args += " -pylon http:slave2 https:true cert:/gabrielvoicu/httpDeployment/server_private_cert.jks serverPort:8886 trustedCa:/gabrielvoicu/httpDeployment/ca.pem";
		args += " -agent AgentB classpath:AgentPingPong";

		//		http://localhost:8886/AgentB

		FlashBoot.main(args.split(" "));
	}
}
