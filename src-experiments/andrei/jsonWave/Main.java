package andrei.jsonWave;

import net.xqhs.flash.json.AgentWaveJson;

@SuppressWarnings("javadoc")
public class Main {
	
	public static void main(String[] args) {
		AgentWaveJson wave = new AgentWaveJson();
		System.out.println(wave.getJson());
		wave.add("a", "1");
		System.out.println(wave.getJson());
		wave.add("b", "2");
		System.out.println(wave.getJson());
		wave.add("a", "3");
		System.out.println(wave.getJson());
		wave.add("a", "4");
		System.out.println(wave.getJson());
		wave.removeFirst("a");
		System.out.println(wave.getJson());
		wave.add("c", "5");
		System.out.println(wave.getJson());
		wave.removeKey("b");
		System.out.println(wave.getJson());
	}
	
}
