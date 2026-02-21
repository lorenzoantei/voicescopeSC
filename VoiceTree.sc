VoiceTree {
	var <window, <listView, <controller;
	var updateTask;

	*new { |controller|
		^super.new.initVoiceTree(controller);
	}

	initVoiceTree { |argController|
		var darkPalette = QPalette.dark;
		controller = argController;
		
		// Finestra laterale (se possibile posizionata a sinistra della principale)
		window = Window("VoiceTree", Rect(5, 50, 180, 500), resizable:true).front;
		window.view.background = Color.grey(0.1);
		
		listView = ListView(window, window.view.bounds.insetBy(2, 2))
			.palette_(darkPalette)
			.background_(Color.grey(0.05))
			.stringColor_(Color.grey(0.8))
			.selectedStringColor_(Color.green)
			.focusColor_(Color.clear)
			.font_(Font("Monaco", 13))
			.items_([])
			.resize_(5)
			.action_({ |menu|
				var nameStr = menu.items[menu.value];
				if(nameStr.notNil) {
					var name = nameStr.asSymbol;
					var voice = controller.voiceDict[name];
					if(voice.notNil) {
						var index = controller.voiceArray.indexOf(voice);
						if(index.notNil) {
							// Deseleziona tutto e seleziona il nodo scelto
							controller.voiceArray.do({ |v| v.selected = false });
							controller.selected = index;
							controller.selectedName = nameStr;
							voice.selected = true;
							// Stampa info in console
							controller.hub.postVoiceState(nameStr, index);
						}
					}
				}
			});
		
		// Polling periodico per aggiornare la lista
		updateTask = Task({
			var lastNames = [];
			inf.do {
				var currentNames = controller.voiceArray.collect(_.name).collect(_.asString);
				if(currentNames != lastNames) {
					{
						listView.items = currentNames;
						lastNames = currentNames;
					}.defer;
				};
				0.3.wait;
			}
		}).play(AppClock);

		window.onClose = { updateTask.stop };
	}
}
