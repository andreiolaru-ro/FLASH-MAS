package gabrielvoicu.httpDeployment;

import net.xqhs.flash.FlashBoot;

public class BootSimpleHttpDeployment
{
	public static void main(String[] args_)
	{
		String args = "";

		args += " -package test.simplePingPong";

		args += " -node node1";
		args += " -pylon http:slave1 serverPort:8885 connectTo:http://localhost:8886/AgentB";
		args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		args += " -agent AgentC classpath:AgentPingPong sendTo:AgentB";

//		http://localhost:8885/AgentA

		args += " -node node2";
		args += " -pylon http:slave2 serverPort:8886";
		args += " -agent AgentB classpath:AgentPingPong";

//		http://localhost:8886/AgentB
//							/AgentC

		FlashBoot.main(args.split(" "));
	}
}
