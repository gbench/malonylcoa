package gbench.webapps.myfuture.broker.model.market;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import org.apache.ignite.table.*;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IgniteCatalog;
import org.apache.ignite.catalog.definitions.TableDefinition;

import gbench.util.jdbc.kvp.IRecord;

import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.webapps.myfuture.broker.model.market.CtpIgniteDB.*;
import static org.apache.ignite.catalog.definitions.ColumnDefinition.column;

public class DeepMarketDataModel {

	@Test
	public void foo() throws InterruptedException {
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, //
				rec -> println(rec)).initialize().start(); //
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void bar() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.withTransaction(client -> {
			final var tbl = client.tables().table("AO2601");
			final var rs = client.sql().execute(null, "select * from system.tables");
			println(tbl);
			println(rs2df(rs));
			println(igniteDB.tableExists("t_mtcars"));
			return null;
		});

	}

	@Test
	public void quz() throws InterruptedException {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final Function<IRecord, Object> tickdata_handler = rec -> {
			igniteDB.put(rec, "InstrumentID", row -> println("row:%s".formatted(row))); // 数据写入
			sleep(1000);
			return null;
		};
		new CtpTickDataMQ(CTP_TOPIC, KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID, tickdata_handler).initialize()
				.start();
		Thread.sleep(1000000); // 等待
	}

	@Test
	public void foo1() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.withTransaction(client -> {
			IgniteCatalog catalog = client.catalog();
			catalog.createTable(TableDefinition.builder("sampleTable3").primaryKey("myKey")
					.columns(column("myKey", ColumnType.INT32), column("myValue", ColumnType.VARCHAR)).build());

			final var myTable = client.tables().table("sampleTable3");
			myTable.keyValueView().put(null, Tuple.create().set("myKey", 1), Tuple.create().set("myValue", "John"));
			Tuple value = myTable.keyValueView().get(null, Tuple.create().set("myKey", 1));
			System.out.println("\nRetrieved value:\n" + value.stringValue("myValue"));
			return null;
		});
	}

	@Test
	public void foo2() {
		final var json = """
				{"ID":536908,"ActionDay":"20251224","AskPrice1":2566.0,"AskPrice2":0.0,"AskPrice3":0.0,"AskPrice4":0.0,"AskPrice5":0.0,"AskVolume1":24,"AskVolume2":0,"AskVolume3":0,"AskVolume4":0,"AskVolume5":0,"AveragePrice":50937.99374408962,"BidPrice1":2565.0,"BidPrice2":0.0,"BidPrice3":0.0,"BidPrice4":0.0,"BidPrice5":0.0,"BidVolume1":58,"BidVolume2":0,"BidVolume3":0,"BidVolume4":0,"BidVolume5":0,"ClosePrice":0.0,"CurrDelta":0.0,"CxxCtpCreateTime":"2025-12-24 13:31:30","ExchangeID":"","ExchangeInstID":"","HighestPrice":2581.0,"InstrumentID":"ao2601","LastPrice":2565.0,"LowerLimitPrice":2343.0,"LowestPrice":2520.0,"OpenInterest":113314.0,"OpenPrice":2520.0,"PreClosePrice":2520.0,"PreDelta":0.0,"PreOpenInterest":135956.0,"PreSettlementPrice":2520.0,"SettlementPrice":0.0,"TradingDay":"20251224","Turnover":7.7026906E9,"UpdateMillisec":500,"UpdateTime":"13:31:29","UpperLimitPrice":2696.0,"Volume":151217}
				""";
		final var proto = REC(json);
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		igniteDB.put(proto, "InstrumentID", row -> println("row:%s".formatted(row)));
	}

	@Test
	public void dropTables() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final var dfm = igniteDB.sqldframe("SELECT TABLE_NAME name from SYSTEM.TABLES");
		final var symbols = dfm.filterBy(rec -> rec.str("name").matches("[A-Z]+\\d{4}([A-Z]+\\d{4,})?"));
		println(symbols);
		symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			final var sql = "DROP TABLE %s".formatted(tbl);
			println("%s".formatted(igniteDB.sqldframe(sql)));
		});
	}

	@Test
	public void selectTbl() {
		final var igniteDB = new CtpIgniteDB(IGNITE_ADDRESS);
		final var dfm = igniteDB.sqldframe("SELECT TABLE_NAME name from SYSTEM.TABLES");
		final var symbols = dfm.filterBy(rec -> rec.str("name").matches("[A-Z]+\\d{4}([A-Z]+\\d{4,})?"));
		symbols.rowS().forEach(e -> {
			final var tbl = e.str(0);
			final var sql = "SELECT ID,UPDATETIME,LASTPRICE,VOLUME FROM %s ORDER BY id limit 10".formatted(tbl);
			println("%s\n:%s\n".formatted(tbl, igniteDB.sqldframe(sql)));
		});
	}

	/**
	 * 
	 * @param millis
	 */
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // 等待
	}

	// 配置参数（对应你给出的参数）
	private static final String CTP_TOPIC = "test_cxx_ctp_topic";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.1.41:9092";
	private static final String KAFKA_CONSUMER_GROUP_ID = "ctp_cxx_ctp_topic_group_ignite-3.10";
	private static final String IGNITE_ADDRESS = "192.168.1.41:10800";

}
