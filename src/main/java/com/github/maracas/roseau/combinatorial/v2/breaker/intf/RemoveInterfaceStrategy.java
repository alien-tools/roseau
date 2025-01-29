package com.github.maracas.roseau.combinatorial.v2.breaker.intf;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.combinatorial.builder.ApiBuilder;
import com.github.maracas.roseau.combinatorial.v2.NewApiQueue;

public class RemoveInterfaceStrategy extends AbstractIntfStrategy {
	public RemoveInterfaceStrategy(InterfaceDecl intf, NewApiQueue queue) {
		super(intf, queue);
	}

	@Override
	protected void applyBreakToMutableApi(API api, ApiBuilder mutableApi) {
		System.out.println("Removing interface " + intf.getPrettyQualifiedName());

		mutableApi.allTypes.remove(intf.getQualifiedName());
	}
}
