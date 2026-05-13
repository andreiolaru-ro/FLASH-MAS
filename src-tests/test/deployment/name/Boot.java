package test.deployment.name;

import net.xqhs.flash.FlashBoot;

public class Boot {
    public static void main(String[] args) {

        String test_args = "-package testing ";
        // node nu are nume. Este null
        // se da load la node dar nu poate sa ii dea start
        //test_args += "-node";

        //pylonul ia numele "-agent"
        //Agent1 devine un parametru al pylonului cu valoarea null
        //Could not load entity [-agent]/[#728890494] of type [pylon].
        //test_args += "-node N1 -pylon -agent Agent1";

        // agentul are numele "classpath:DummyAgent"
        //Could not load entity [classpath:DummyAgent]/[#728890494] of type [agent].
        //test_args += "-node N1 -agent classpath:DummyAgent param:val";

        //numele nodului este "-pylon", pylon-ul se creeaza automat default, iar agentul are numele "-shard"
        //Could not load entity [-shard]/[#728890494] of type [agent].
        //node loaded: [-pylon]
        //EchoTesting devine un parametru al agentului cu valoarea null
        //test_args += "-node -pylon -agent -shard EchoTesting exit:2";


        test_args += "-node -agent - classpath:AgentPingPong param:val";
        FlashBoot.main(test_args.split(" "));
    }
}
