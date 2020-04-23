package ClientProviderSimulationCompositeAgents;


import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.lang.reflect.Array;
import java.util.Random;

import java.util.ArrayList;

enum ProviderServices {

    EVEN_NUMBERS,
    ODD_NUMBERS,
    FIBONACCI,
    NUMBER_MULTIPLES,
    QUADRATIC_EQUATIONS

}


public class ClientProviderSimulationCompositeAgents {


    public static int USERS_COUNT = 50;
    public static int PROVIDER_COUNT = 50;
    public static final int SERVICES_COUNT = 5;


    public static ArrayList<UserCompositeAgent> createUserAgentList() {
        ArrayList<UserCompositeAgent> agentList = new ArrayList<>(USERS_COUNT);
        for(int i = 0; i < USERS_COUNT; i++) {
            MultiTreeMap configuration = new MultiTreeMap();
            configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "User " +  Integer.toString(i));
            agentList.add(new UserCompositeAgent( configuration));
        }

        return agentList;
    }

    public static void addContextToUserAgentsList(LocalSupport pylon, ArrayList<UserCompositeAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++) {
            agentList.get(i).addContext(pylon.asContext());
        }
    }

    public static void addShardsToUsersList( ArrayList<UserCompositeAgent> users) {

        for(int i = 0; i < users.size(); i++){

            ArrayList<AgentShardCore> shards = new ArrayList<>();
            shards.add(new UserRequestShardForComposite(AgentShardDesignation.customShard(UserRequestShardForComposite.USER_REQUEST_SHARD_DESIGNATION)));
            shards.add(new LocalSupport.SimpleLocalMessaging());
            users.get(i).addShards(shards);

        }
    }


    public static void addRandomRequests(UserCompositeAgent agent, ProviderServices service, ProviderServices[] services) {

        int random_service_index1 = 0;
        do {
            random_service_index1 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index1 == service.ordinal());


        agent.addRequest(services[random_service_index1]);

        int random_service_index2 = 0;
        do {
            random_service_index2 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index2 == service.ordinal()  || random_service_index2 == random_service_index1 );

        agent.addRequest(services[random_service_index2]);

    }


    public static void addRequestsToUsersList(ArrayList<UserCompositeAgent> users) {

        int chunk = 1;
        if (users.size() >= SERVICES_COUNT)
            chunk = users.size() / SERVICES_COUNT;

        ProviderServices service = ProviderServices.EVEN_NUMBERS;
        ProviderServices[] services =  ProviderServices.values();

        for(int i = 0; i < users.size(); i += chunk) {
            for(int j = i; j < i + chunk; j++) {
                UserCompositeAgent agent =  users.get(j);

                /* Add the mandatory request for service from one of the five categories */
                agent.addRequest(service);

                /*  Add the remaining 2 shards random */
                addRandomRequests(agent, service, services);
            }
            if(service.ordinal() + 1 < SERVICES_COUNT) {
                service = services[service.ordinal() + 1];

            }
        }


    }



    public static ArrayList<ProviderCompositeAgent> createProviderAgentList() {
        ArrayList<ProviderCompositeAgent> agentList = new ArrayList<>(PROVIDER_COUNT);
        for(int i = 0; i < PROVIDER_COUNT; i++) {
            MultiTreeMap configuration = new MultiTreeMap();
            configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Provider " +  Integer.toString(i));
            agentList.add(new ProviderCompositeAgent( configuration));
        }

        return agentList;
    }

    public static void addContextToProviderAgentsList(LocalSupport pylon, ArrayList<ProviderCompositeAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++) {
            agentList.get(i).addContext(pylon.asContext());
        }
    }


    public static void addRandomShards(ProviderCompositeAgent agent, ProviderServices service, ProviderServices[] services) {

        int random_service_index1 = 0;
        do {
            random_service_index1 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index1 == service.ordinal());

        ArrayList<AgentShardCore> shards = new ArrayList<>();
        AgentShardCore shard2 = getShardFromService(services[random_service_index1]);
        shards.add(shard2);

        int random_service_index2 = 0;
        do {
            random_service_index2 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index2 == service.ordinal()  || random_service_index2 == random_service_index1 );

        AgentShardCore shard3 = getShardFromService(services[random_service_index2]);
        shards.add(shard3);

        agent.addShards(shards);
    }



    public static void addShardsToProvidersList(ArrayList<ProviderCompositeAgent> providers) {

        int chunk = providers.size() / SERVICES_COUNT;
        ProviderServices service = ProviderServices.EVEN_NUMBERS;
        ProviderServices[] services =  ProviderServices.values();

        for(int i = 0; i < providers.size(); i += chunk) {
            for(int j = i; j < i + chunk; j++) {
                ProviderCompositeAgent agent = providers.get(j);

                ArrayList<AgentShardCore> shards = new ArrayList<>();
                /* Add messaging shard */
                shards.add(new LocalSupport.SimpleLocalMessaging());

                /* Add the mandatory shard from one of the five categories */
                AgentShardCore shard1 = getShardFromService(service);
                shards.add(shard1);
                agent.addShards(shards);

                /*  Add the remaining 2 shards random */
                addRandomShards(agent, service, services);
            }
            if (service.ordinal() + 1 < SERVICES_COUNT) {
                service = services[service.ordinal() + 1];
            }
        }

    }

    public static AgentShardCore getShardFromService(ProviderServices serice){

        switch(serice) {
            case EVEN_NUMBERS:
                return new EvenNumbersShardForComposite(AgentShardDesignation.customShard(EvenNumbersShardForComposite.EVEN_NUMBERS_SHARD_DESIGNATION));

            case ODD_NUMBERS:
                return new OddNumbersShardForComposite(AgentShardDesignation.customShard(OddNumbersShardForComposite.ODD_NUMBERS_SHARD_DESIGNATION));

            case FIBONACCI:
                return new FibonacciShardForComposite(AgentShardDesignation.customShard(FibonacciShardForComposite.FIBONACCI_SHARD_DESIGNATION));

            case NUMBER_MULTIPLES:
                return  new NumberMultiplesCountShardForComposite(AgentShardDesignation.customShard(NumberMultiplesCountShardForComposite.NUMBER_MULTIPLES_SHARD_DESIGNATION));

            case QUADRATIC_EQUATIONS:
                return  new QuadraticEquationSolverShardForComposite(AgentShardDesignation.customShard(QuadraticEquationSolverShardForComposite.QUADRATIC_EQUATION_SHARD_DESIGNATION));

            default:
                return  new EvenNumbersShardForComposite(AgentShardDesignation.customShard(EvenNumbersShardForComposite.EVEN_NUMBERS_SHARD_DESIGNATION));
        }
    }


    public static void main(String[] args) {
        LocalSupport pylon = new LocalSupport();


        /*Create supervisor agent*/
        MultiTreeMap configuration = new MultiTreeMap();
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Supervisor");
        SupervisorCompositeAgent supervisorAgent = new SupervisorCompositeAgent(configuration);

        supervisorAgent.addContext(pylon.asContext());

        ArrayList<AgentShardCore> shards = new ArrayList<>();
        shards.add(new LocalSupport.SimpleLocalMessaging());
        supervisorAgent.addShards(shards);

        supervisorAgent.setUsersCount(USERS_COUNT);
        supervisorAgent.setProvidersCount(PROVIDER_COUNT);

        /* Create user agents */
        ArrayList<UserCompositeAgent> users = createUserAgentList();
        addContextToUserAgentsList(pylon, users);
        addShardsToUsersList(users);
        addRequestsToUsersList(users);


        /* Create provider agents*/
        ArrayList<ProviderCompositeAgent> providers = createProviderAgentList();
        addContextToProviderAgentsList(pylon, providers);
        addShardsToProvidersList(providers);


        /* Start all */
        supervisorAgent.start();

        for(UserCompositeAgent user: users) {
            user.start();
        }

        for(ProviderCompositeAgent provider : providers) {
            provider.start();
        }

        /* Run all */

        supervisorAgent.run();

        for(UserCompositeAgent user: users) {
            user.run();
        }

        for(ProviderCompositeAgent provider : providers) {
            provider.run();
        }

        /* Stop all */

        supervisorAgent.stop();

        for(UserCompositeAgent user: users) {
            user.stop();
        }

        for(ProviderCompositeAgent provider : providers) {
            provider.stop();
        }
    }
}
