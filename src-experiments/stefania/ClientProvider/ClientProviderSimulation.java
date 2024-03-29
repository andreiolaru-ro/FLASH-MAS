package stefania.ClientProvider;

import static stefania.ClientProvider.Constants.PROVIDER_COUNT;
import static stefania.ClientProvider.Constants.SERVICES_COUNT;
import static stefania.ClientProvider.Constants.USERS_COUNT;

import java.util.ArrayList;
import java.util.Random;

import mpi.MPI;
import mpi.MPIException;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.mpi.MPISupport;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

enum ProviderServices {

    EVEN_NUMBERS,
    ODD_NUMBERS,
    FIBONACCI,
    NUMBER_MULTIPLES,
    QUADRATIC_EQUATIONS

}

//class ClientProviderNode extends Node
//{
//    public static final int MAX_THREADS = 31;
//    public ClientProviderNode(MultiTreeMap configuration)
//    {
//        super(configuration);
//    }
//
//    public void registerUsersInNode( ArrayList<UserAgent> agentList) {
//
//        for(Agent agent : agentList ) {
//            registerEntity("Agent",agent, agent.getName() );
//        }
//    }
//
//    public void registerProvidersInNode( ArrayList<ProviderAgent> agentList) {
//
//        for(Agent agent : agentList ) {
//            registerEntity("Agent",agent, agent.getName() );
//        }
//    }
//
//    public void registerSupervisorInNode(SupervisorAgent supervisorAgent) {
//        registerEntity("Agent", supervisorAgent, supervisorAgent.getName());
//    }
//
//    @Override
//    public void run() {
//        //long startTime = System.nanoTime();
//
//        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
//        li("Starting node [].", name);
//        for(Entity<?> entity : entityOrder) {
//            if(entity instanceof Agent) {
//                lf("running an entity...");
//                Runnable agentTask = () -> entity.run();
//
//                pool.execute(agentTask);
//            } else {
//                lf("running an entity...");
//                entity.run();
//            }
//        }
//        li("Node [] is running.", name);
//
//        pool.shutdown();
//
//        //System.out.println( "Simulation time " + (System.nanoTime() - startTime));
//
//    }
//}


public class ClientProviderSimulation {

//    public static ArrayList<UserAgent> createUserAgentList() {
//        ArrayList<UserAgent> agentList = new ArrayList<>(USERS_COUNT);
//        for(int i = 0; i < USERS_COUNT; i++) {
//            agentList.add(new UserAgent( "User " +  Integer.toString(i)));
//        }
//
//        return agentList;
//    }
//
//    public static ArrayList<ProviderAgent> createProviderAgentList() {
//        ArrayList<ProviderAgent> agentList = new ArrayList<>(PROVIDER_COUNT);
//        for(int i = 0; i < PROVIDER_COUNT; i++) {
//            agentList.add(new ProviderAgent( "Provider " +  Integer.toString(i)));
//        }
//
//        return agentList;
//    }

//    public static void addContextToUserAgentsList(LocalSupport pylon, ArrayList<UserAgent> agentList) {
//
//        for(int i = 0; i < agentList.size(); i++) {
//            agentList.get(i).addContext(pylon.asContext());
//        }
//    }
//
//    public static void addContextToProviderAgentsList(LocalSupport pylon, ArrayList<ProviderAgent> agentList) {
//
//        for(int i = 0; i < agentList.size(); i++) {
//            agentList.get(i).addContext(pylon.asContext());
//        }
//    }
//
//
//    public static void addShardsToUsersList( ArrayList<UserAgent> users) {
//
//        for(int i = 0; i < users.size(); i++){
//            users.get(i).addMessagingShard(new LocalSupport.SimpleLocalMessaging());
//            users.get(i).addUserRequestShard(new UserRequestShard(AgentShardDesignation.customShard(UserRequestShard.USER_REQUEST_SHARD_DESIGNATION)));
//        }
//    }

    public static void addRandomShards(ProviderAgent agent, ProviderServices service, ProviderServices[] services) {

        int random_service_index1 = 0;
        do {
            random_service_index1 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index1 == service.ordinal());


        AgentShardCore shard2 = getShardFromService(services[random_service_index1]);
        agent.addShard(service, shard2);

        int random_service_index2 = 0;
        do {
            random_service_index2 =  new Random().nextInt(SERVICES_COUNT - 1);
        }while(random_service_index2 == service.ordinal()  || random_service_index2 == random_service_index1 );

        AgentShardCore shard3 = getShardFromService(services[random_service_index2]);
        agent.addShard(service, shard3);

    }


//    public static void addShardsToProvidersList(ArrayList<ProviderAgent> providers) {
//
//        int chunk = providers.size() / SERVICES_COUNT;
//        ProviderServices service = ProviderServices.EVEN_NUMBERS;
//        ProviderServices[] services =  ProviderServices.values();
//
//        for(int i = 0; i < providers.size(); i += chunk) {
//            for(int j = i; j < i + chunk; j++) {
//                ProviderAgent agent = providers.get(j);
//
//                /* Add messaging shard */
//                agent.addShard(new LocalSupport.SimpleLocalMessaging());
//
//                /* Add the mandatory shard from one of the five categories */
//                AgentShardCore shard1 = getShardFromService(service);
//                agent.addShard(service, shard1);
//
//                /*  Add the remaining 2 shards random */
//                addRandomShards(agent, service, services);
//            }
//            if (service.ordinal() + 1 < SERVICES_COUNT) {
//                service = services[service.ordinal() + 1];
//            }
//        }
//
//    }


    public static void addRandomRequests(UserAgent agent, ProviderServices service, ProviderServices[] services) {

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


    public static void addRequestsToUsersList(ArrayList<UserAgent> users) {

        int chunk = 1;
        if (users.size() >= SERVICES_COUNT)
            chunk = users.size() / SERVICES_COUNT;

        ProviderServices service = ProviderServices.EVEN_NUMBERS;
        ProviderServices[] services =  ProviderServices.values();

        for(int i = 0; i < users.size(); i += chunk) {
            for(int j = i; j < i + chunk; j++) {
                UserAgent agent =  users.get(j);

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

    public static AgentShardCore getShardFromService(ProviderServices serice){

        switch(serice) {
            case EVEN_NUMBERS:
                return new EvenNumbersShard(AgentShardDesignation.customShard(EvenNumbersShard.EVEN_NUMBERS_SHARD_DESIGNATION));

            case ODD_NUMBERS:
                return new OddNumbersShard(AgentShardDesignation.customShard(OddNumbersShard.ODD_NUMBERS_SHARD_DESIGNATION));

            case FIBONACCI:
                return new FibonacciShard(AgentShardDesignation.customShard(FibonacciShard.FIBONACCI_SHARD_DESIGNATION));

            case NUMBER_MULTIPLES:
                return  new NumberMultiplesCountShard(AgentShardDesignation.customShard(NumberMultiplesCountShard.NUMBER_MULTIPLES_SHARD_DESIGNATION));

            case QUADRATIC_EQUATIONS:
                return  new QuadraticEquationSolverShard(AgentShardDesignation.customShard(QuadraticEquationSolverShard.QUADRATIC_EQUATION_SHARD_DESIGNATION));

            default:
                return  new EvenNumbersShard(AgentShardDesignation.customShard(EvenNumbersShard.EVEN_NUMBERS_SHARD_DESIGNATION));
        }
    }

    public static void main(String[] args) throws MPIException {

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        if (rank == 0) {
            SupervisorAgent supervisorAgent = new SupervisorAgent("Supervisor [0]");
            supervisorAgent.addContext(pylon.asContext());
            AsynchronousMPIMessaging asynchronousMPIMessaging = new AsynchronousMPIMessaging();
            asynchronousMPIMessaging.addContext(supervisorAgent.asContext());
            supervisorAgent.addMessagingShard(asynchronousMPIMessaging);
            supervisorAgent.setUsersCount(USERS_COUNT);
            supervisorAgent.setProvidersCount(PROVIDER_COUNT);
            System.out.println("Starting supervisor");
            supervisorAgent.start();
        } else if (rank <= USERS_COUNT) {
            UserAgent userAgent = new UserAgent("User [" + rank + "]");
            userAgent.addContext(pylon.asContext());
            AsynchronousMPIMessaging asynchronousMPIMessaging = new AsynchronousMPIMessaging();
            asynchronousMPIMessaging.addContext(userAgent.asContext());
            userAgent.addMessagingShard(asynchronousMPIMessaging);
            // add requests
            if (rank % 2 == 0) {
//                userAgent.addRequest(ProviderServices.EVEN_NUMBERS);
//                userAgent.addRequest(ProviderServices.FIBONACCI);
////                userAgent.addRequest(ProviderServices.FIBONACCI);
                userAgent.addRequest(ProviderServices.FIBONACCI);
                userAgent.addRequest(ProviderServices.QUADRATIC_EQUATIONS);
//                userAgent.addRequest(ProviderServices.ODD_NUMBERS);
            } else {
                userAgent.addRequest(ProviderServices.EVEN_NUMBERS);
                userAgent.addRequest(ProviderServices.ODD_NUMBERS);
                userAgent.addRequest(ProviderServices.NUMBER_MULTIPLES);
            }
            System.out.println("Starting user");
            userAgent.start();
        } else {
            ProviderServices[] services =  ProviderServices.values();

            ProviderAgent providerAgent = new ProviderAgent("Provider [" + rank + "]");
            providerAgent.addContext(pylon.asContext());
            AsynchronousMPIMessaging asynchronousMPIMessaging = new AsynchronousMPIMessaging();
            asynchronousMPIMessaging.addContext(providerAgent.asContext());
            providerAgent.addMessagingShard(asynchronousMPIMessaging);
            // add shards
            if (rank % 2 == 0) {
                providerAgent.addShard(ProviderServices.EVEN_NUMBERS, getShardFromService(ProviderServices.EVEN_NUMBERS));
                providerAgent.addShard(ProviderServices.FIBONACCI, getShardFromService(ProviderServices.FIBONACCI));
                providerAgent.addShard(ProviderServices.ODD_NUMBERS, getShardFromService(ProviderServices.ODD_NUMBERS));
//                providerAgent.addShard(ProviderServices.NUMBER_MULTIPLES, getShardFromService(ProviderServices.NUMBER_MULTIPLES));
            } else {
//                providerAgent.addShard(ProviderServices.EVEN_NUMBERS, getShardFromService(ProviderServices.EVEN_NUMBERS));
//                providerAgent.addShard(ProviderServices.FIBONACCI, getShardFromService(ProviderServices.FIBONACCI));
//                providerAgent.addShard(ProviderServices.ODD_NUMBERS, getShardFromService(ProviderServices.ODD_NUMBERS));
                providerAgent.addShard(ProviderServices.NUMBER_MULTIPLES, getShardFromService(ProviderServices.NUMBER_MULTIPLES));
                providerAgent.addShard(ProviderServices.QUADRATIC_EQUATIONS, getShardFromService(ProviderServices.QUADRATIC_EQUATIONS));
            }
            System.out.println("starting provider");
            providerAgent.start();
        }

        MPI.Finalize();
//        MultiTreeMap configuration = new MultiTreeMap();
//        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "testNode");
//        ClientProviderNode node = new ClientProviderNode(configuration);
//        LocalSupport pylon = new LocalSupport();
//
//
//        /*  Pot face mai multe scenarii: 1. pentru fiecare user, exista un provider liber care il poate ajuta;
//         *                              2. exista mai putini provideri decat useri */
//
//
//        /*Create supervisor agent*/
//        SupervisorAgent supervisorAgent = new SupervisorAgent("Supervisor");
//        supervisorAgent.addContext(pylon.asContext());
//        supervisorAgent.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
//        supervisorAgent.setUsersCount(USERS_COUNT);
//        supervisorAgent.setProvidersCount(PROVIDER_COUNT);
//
//        /* Create user agents */
//        ArrayList<UserAgent> users = createUserAgentList();
//        addContextToUserAgentsList(pylon, users);
//        addShardsToUsersList(users);
//        addRequestsToUsersList(users);
//
//
//        /* Create provider agents*/
//        ArrayList<ProviderAgent> providers = createProviderAgentList();
//        addContextToProviderAgentsList(pylon, providers);
//        addShardsToProvidersList(providers);
//
//
//        node.registerSupervisorInNode(supervisorAgent);
//        node.registerUsersInNode( users);
//        node.registerProvidersInNode(providers);
//
//        node.start();
//        node.run();
//        node.stop();


    }


}
