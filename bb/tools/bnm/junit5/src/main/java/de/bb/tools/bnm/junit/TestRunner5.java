package de.bb.tools.bnm.junit;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;

public class TestRunner5 implements TestRunner {

	private static final Throwable DUNNO = new Throwable("DUNNO");
	boolean ok = true;
	int count;
	int successful;
	int skipped;
	int failed;
	int aborted;

	int localCount;
	int localSuccessful;
	int localFailed;
	int localAborted;

	long localStart;
	long testStart;

	Map<TestExecutionResult, UniqueId> errors = new LinkedHashMap<>();

	@Override
	public boolean runTests(ClassLoader cl, File dir, ArrayList<String> files) throws Exception {
		UniqueId uid = UniqueId.forEngine("junit-jupiter");
		ConfigurationParameters cp = new ConfigurationParameters() {
			@Override
			public Optional<String> get(String arg0) {
				switch (arg0) {
				case "junit.jupiter.displayname.generator.default":
					return Optional.of("org.junit.jupiter.api.DisplayNameGenerator$Standard");
				case "junit.jupiter.testinstance.lifecycle.default":
					return Optional.of("PER_METHOD");
				}
				return Optional.empty();
			}

			@Override
			public Optional<Boolean> getBoolean(String arg0) {
				switch (arg0) {
				case "junit.jupiter.execution.parallel.enabled":
					return Optional.of(Boolean.FALSE);
				}
				return Optional.empty();
			}

			@Override
			public int size() {
				return 0;
			}
		};
		JupiterConfiguration jc = new DefaultJupiterConfiguration(cp);
		JupiterEngineDescriptor jed = new JupiterEngineDescriptor(uid, jc);

		Map<String, Integer> map = new HashMap<>();
		for (String file : files) {
			String classname0 = file.replace(".class", "").replace('/', '.').replace('\\', '.');
			Class<?> clz = cl.loadClass(classname0);
			String classname = clz.getName();
			UniqueId cuid = uid.append("class", classname);
			ClassTestDescriptor ctd = new ClassTestDescriptor(cuid, clz, jc);
			jed.addChild(ctd);

			for (Method m : clz.getDeclaredMethods()) {
				if (m.getAnnotation(Test.class) != null) {
					++count;

					if (m.getAnnotation(Disabled.class) != null) {
						map.put(classname, map.getOrDefault(classname, 0) + 1);
						++skipped;
						continue;
					}

					UniqueId muid = cuid.append("method", m.getName());
					TestMethodTestDescriptor tmtd = new TestMethodTestDescriptor(muid, clz, m, jc);
					ctd.addChild(tmtd);
				}
			}
		}

		EngineExecutionListener eel = new EngineExecutionListener() {
			@Override
			public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
				String t;
				Status status = testExecutionResult.getStatus();

				switch (testDescriptor.getType()) {
				case TEST:
					UniqueId uid = testDescriptor.getUniqueId();
					String mname = toName(uid);
					t = timeToString(System.currentTimeMillis() - testStart);
					System.out.println(mname + "\tTime elapsed: " + t + " sec\t <<< " + status.name());
					switch (status) {
					case SUCCESSFUL:
						++successful;
						++localSuccessful;
						break;
					case FAILED:
						++failed;
						++localFailed;
						errors.put(testExecutionResult, testDescriptor.getUniqueId());
						break;
					case ABORTED:
						++aborted;
						++localAborted;
						errors.put(testExecutionResult, testDescriptor.getUniqueId());
					}
					break;
				case CONTAINER:
					UniqueId uniqueId = testDescriptor.getUniqueId();
					Segment lastSegment = uniqueId.getLastSegment();
					if (uniqueId.getSegments().get(0) != lastSegment) {
						t = timeToString(System.currentTimeMillis() - localStart);
						System.out.println("--------------------------------------------------------------------------------");
						String testName = lastSegment.getValue();
						System.out.println(testName + ":");
						System.out.println("Tests run: " + localCount + ", Skipped: " + map.getOrDefault(testName, 0) + ", Failures: " + localFailed + ", Aborted: "
								+ localAborted + ", Time elapsed: " + t + " sec");
						System.out.println("--------------------------------------------------------------------------------");
					}
				default:
				}

			}

			@Override
			public void executionStarted(TestDescriptor testDescriptor) {
				switch (testDescriptor.getType()) {
				case TEST:
					++localCount;
					testStart = System.currentTimeMillis();
					break;
				case CONTAINER:
					UniqueId uniqueId = testDescriptor.getUniqueId();
					if (uniqueId.getSegments().get(0) != uniqueId.getLastSegment()) {
						System.out.println("Running " + uniqueId.getLastSegment().getValue());
						localCount = 0;
						localSuccessful = 0;
						localFailed = 0;
						localAborted = 0;
						localStart = System.currentTimeMillis();
					}
				default:
				}
			}
		};
		ExecutionRequest er = new ExecutionRequest(jed, eel, cp);
		TestEngine te = new JupiterTestEngine();

		long start = System.currentTimeMillis();
		te.execute(er);

		String t = timeToString(System.currentTimeMillis() - start);

		if (!errors.isEmpty()) {
			System.out.println("================================================================================");
			System.out.println("Failed tests:");
			for (Entry<TestExecutionResult, UniqueId> e : errors.entrySet()) {
				Optional<Throwable> optThrowable = e.getKey().getThrowable();
				Throwable throwable = optThrowable.orElseGet(() -> DUNNO);
				UniqueId unique = e.getValue();
				String mname = toName(unique);
				System.out.println(mname + ": " + throwable);
				if (optThrowable.isPresent()) {
					StackTraceElement[] sts = throwable.getStackTrace();
					for (StackTraceElement st : sts) {
						System.out.println("\t\t" + st);
						if (mname.equals(st.getClassName() + "." + st.getMethodName() + "()"))
							break;
					}
				}
			}
		}
		
		System.out.println("================================================================================");
		System.out.println("Total run: " + count + ", Skipped: " + skipped + ", Failures: " + failed + ", Aborted: " + aborted
				+ ", Time elapsed: " + t + " sec");
		System.out.println("================================================================================");

		return count == successful + skipped;
	}

	static String timeToString(long took) {
		String t = Long.toString(took % 1000);
		while (t.length() < 3)
			t = "0" + t;
		t = (took / 1000) + "." + t;
		return t;
	}

	static String toName(UniqueId uid) {
		String method = uid.getLastSegment().getValue();
		String cls = uid.removeLastSegment().getLastSegment().getValue();
		return cls + "." + method + "()";
	}

}
