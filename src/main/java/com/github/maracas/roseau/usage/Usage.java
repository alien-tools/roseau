package com.github.maracas.roseau.usage;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.diff.APIDiff;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class Usage {
	private final CtModel model;
	private final API api;

	public Usage(CtModel model, API api) {
		this.model = model;
		this.api = api;
	}

	List<Use> inferUses() {
		// FIXME: Not sure why the former doesn't work?!
		//scanner.scan(model);
		UsageVisitor visitor = new UsageVisitor(api);
		model.getRootPackage().accept(visitor);
		return visitor.getUses();
	}

	public static void main(String[] args) throws Exception{
		var factory = new SpoonAPIFactory();
		var v1 = API.fromJson(Path.of("guava-21.0.json"), factory);
		var v2 = API.fromJson(Path.of("guava-31.1.json"), factory);
		var bcs = new APIDiff(v1, v2).diff();

		var client = SpoonUtils.buildModel(Path.of("/home/dig/guava/guava/guava-tests/test"), Duration.ofSeconds(30));
		var usage = new Usage(client, v1);
		var uses = usage.inferUses();

		System.out.println(uses.stream().map(Use::toString).collect(Collectors.joining("\n")));
		System.out.println(uses.size() + " uses");
	}
}
