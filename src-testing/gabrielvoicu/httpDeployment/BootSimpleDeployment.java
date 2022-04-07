package gabrielvoicu.httpDeployment;

import net.xqhs.flash.FlashBoot;

public class BootSimpleDeployment
{
	public static void main(String[] args_)
	{
		String args = "";

		args += " -package example.simplePingPong";

		args += " -node node1";
		args += " -pylon http:slave1 serverPort:8885 connectTo:http://localhost:8886";
		args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";

//		http://localhost:8885/AgentA

		args += " -node node2";
		args += " -pylon http:slave2 serverPort:8886 connectTo:http://localhost:8885";
		args += " -agent AgentB classpath:AgentPingPong";

//		http://localhost:8886/AgentB
//							/AgentC

		FlashBoot.main(args.split(" "));
	}
}
