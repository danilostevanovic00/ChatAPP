package rs.raf.pds.v4.z5.messages;

import com.esotericsoftware.kryo.Kryo;

public class KryoUtil {
	public static void registerKryoClasses(Kryo kryo) {
		kryo.register(String.class);
		kryo.register(String[].class);
		kryo.register(Login.class);
		kryo.register(ChatMessage.class);
		kryo.register(PrivateMessage.class);
		kryo.register(WhoRequest.class);
		kryo.register(WhoRoomRequest.class);
		kryo.register(ListUsers.class);
		kryo.register(ListRooms.class);
		kryo.register(InfoMessage.class);
		kryo.register(CreateRoomMessage.class);
		kryo.register(JoinRoomMessage.class);
	}
}
