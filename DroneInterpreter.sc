DroneInterpreter {

	var <>mainwin, states, <>textview, <>postview, mode; // textview accessed from DroneStates
	var <rect, <postrect, <lastCommand;
	var lineview;
	
	*new { |hub, mode|
		^super.new.initDroneInterpreter(hub, mode);
	}

			initDroneInterpreter { |arghub, argmode|
		var leftborder;
		var hub = arghub;
		var droneDict = ();
		var thiscommand;
		var drones = hub.drones;
		var selected = drones.selected;
		var selectedName = try{drones.droneArray[selected].name.asString};
		
		// Palette scura forzata per risolvere problemi di tema Linux/Qt
		var darkPalette = QPalette.dark; 
		darkPalette.windowText = Color.green;
		darkPalette.window = Color.black;
		darkPalette.button = Color.grey(0.2);
		darkPalette.buttonText = Color.green;
		
		if(selectedName == nil, {selectedName = "oxo"});
		
		mainwin = hub.window;
		leftborder = mainwin.bounds.height;
		mode = argmode;
		rect = Rect(leftborder+1, 30, Window.screenBounds.width-leftborder, mainwin.bounds.height-250);
		postrect = Rect(leftborder+1, rect.height+11, Window.screenBounds.width-leftborder, mainwin.bounds.height-rect.height-10);
		states = hub.states;

		// --- SETUP FINESTRA ---
		if(mode == \dev, { 
			mainwin = Window("code", Rect(mainwin.bounds.width, 0, 400, mainwin.bounds.height), resizable:true, border:true).front;
			mainwin.onClose_({ hub.window.close; drones.killAll; });
			
			// SFONDO NERO e PALETTE SCURA
			mainwin.view.background = Color.black;
			mainwin.view.palette = darkPalette; 
			
			leftborder = 0;
			rect = Rect(leftborder+1, 30, 400, mainwin.bounds.height-60-240);
			postrect = Rect(leftborder+1, rect.height+11, 400, mainwin.bounds.height-rect.height-64);

			// Lancio GUI personalizzata
			{ if("DroneGUI".asSymbol.asClass.notNil) { DroneGUI.new(hub) } }.defer(0.1);

		}, {
			mainwin.bounds = Window.screenBounds;
		});

		// --- WIDGET ---

		// 1. QUIT Button
		Button(mainwin,Rect(rect.left+5, 5 , 50, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette) // Forza palette
			.states_([["Quit", Color.white, Color.grey(0.2)]])
			.action_({ arg butt; ~drones.quit; });

		// 2. HELP Button
		Button(mainwin,Rect(rect.left+65, 5 , 50, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette)
			.states_([["Help", Color.white, Color.grey(0.2)]])
			.action_({ arg butt; "open http://thormagnusson.github.io/threnoscope".unixCmd; });

		// 3. Label "Window mode"
		StaticText(mainwin, Rect(rect.left+125, 5, 120, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette) // Forza palette
			.stringColor_(Color.white)
			.string_("Window mode:");

		// 4. Menu Mode
		PopUpMenu(mainwin,Rect(rect.left+210, 5, 70, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette) // Forza palette
			.background_(Color.grey(0.2))
			.stringColor_(Color.white)
			.items_([ "perform", "performWin", "dev", "displayFS", "displayWin"])
			.action_({ arg menu;
				hub.drones.mode(menu.item.asSymbol);
			});

		// 5. Label "Main vol"
		StaticText(mainwin, Rect(rect.left+290, 5, 60, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette)
			.stringColor_(Color.white)
			.string_("Main vol:");

		// 6. Slider Volume
		Slider(mainwin, Rect(rect.left+350, 5, 150, 20))
			.palette_(darkPalette)
			.background_(Color.grey(0.2))
			.knobColor_(Color.white)
			.value_(0.5)
    		.action_({arg slider; Server.default.volume_(((0.00001+slider.value)*2).ampdb.postln;)});

		// 7. TUNING Button
		Button(mainwin,Rect(rect.left+505, 5, 50, 20))
			.font_(Font("Monaco", 11))
			.palette_(darkPalette)
			.states_([["Tuning", Color.white, Color.grey(0.2)]])
			.action_({ arg butt;
	    		var t;
	    		t = TuningTheory.new;
	    		t.createGUI;
			});

		// 8. TEXTVIEW PRINCIPALE (Input Codice)
		textview = TextView.new(mainwin, rect)
				.palette_(darkPalette)
				.focusColor_(Color.clear)
				.hasVerticalScroller_(true)
				.string_("")
				.resize_(2)
				.font_(Font("Helvetica", 16 ))
				.background_(Color.black)
				.stringColor_(Color.green) // Testo verde stile hacker
.keyDownAction_({|doc, char, mod, unicode, keycode |
					var linenr, string;
					//[doc, char, mod, unicode, keycode].postln;
					// evaluating code (the next line will use .isAlt, when that is available)
					if((mod & 524288 == 524288) && (keycode==124), { // alt + right
						linenr = doc.string[..doc.selectionStart-1].split($\n).size;
						string = doc.selectedString;
						thiscommand = string[1..string.size].replace("   ", "").replace("  ", "");
						this.opInterpreter(string);
						nil;
					});
					if((mod & 524288 == 524288) && (keycode==123), { // alt + left
						textview.string_(textview.string++thiscommand);
						nil;
					});
					
					if((mod & 524288 == 524288) && (keycode==126), { // alt + up
						
						var name, collectivename;
						selected = (selected-1).clip(0, drones.droneArray.size-1);
						name = drones.droneArray[selected].name.asString;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								hub.drones.chordDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.satellitesDict[key].selected = true;
									});
								});
								//satellitesDict[name[0..6].asSymbol].selected = true;
							});	
							if(name.contains("grp_"), {
								//var grpname = key.asString[0..key.asString.findAll("_")[1]-1];
								hub.drones.groupDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.groupDict[key].selected = true;
									});
								});
							});	
							drones.selectedName = collectivename; // only the name of the group
							block{ arg break;
								drones.droneArray.do({ arg drone, i;
									if(drone.name.asString.contains("_"), {
										if((drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename), {
											selected = i;
											break.value();
										});
									});
								});											};
						}, { 
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});
																		drones.selected = selected;
						//drones.droneArray[selected].selected = true;
						[\SELECTED_, drones.droneArray[selected].name].postln;
						
						selectedName = drones.droneArray[selected].name.asString;
						hub.postDroneState(selectedName, selected);
//						Document.listener.string = ""; // clear post window
//						string = "~"++selectedName++"\n"++
//						"~"++selectedName++".type = \\"++drones.droneArray[selected].type++"\n"++
//						"~"++selectedName++".tonic = "++drones.droneArray[selected].tonic++"\n"++
//						"~"++selectedName++".harmonics = "++drones.droneArray[selected].harmonics++"\n"++
//						"~"++selectedName++".amp = "++drones.droneArray[selected].amp++"\n"++
//						"~"++selectedName++".speed = "++(drones.droneArray[selected].speed*1000)++"\n"++
//						"~"++selectedName++".length = "++(drones.droneArray[selected].length*360/(2*pi))++"\n"++
//						"~"++selectedName++".angle = "++(drones.droneArray[selected].angle*360/(2*pi))++"\n"++
//						"~"++selectedName++".degree = "++drones.droneArray[selected].degree++"\n"++
//						"~"++selectedName++".ratio = "++drones.droneArray[selected].ratio++"\n"++
//						"~"++selectedName++".env = "++drones.droneArray[selected].env++"\n"++
//						"~"++selectedName++".octave = "++drones.droneArray[selected].octave++"\n";
//						Document.listener.string = string; // add info
//						if(hub.post, { hub.interpreter.postview.string_(string) });
						nil;
					});
					
					
					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						var name, collectivename;
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = (selected+1).clip(0, drones.droneArray.size-1);
						name = drones.droneArray[selected].name.asString;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								hub.drones.chordDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.satellitesDict[key].selected = true;
									});
								});
							});	
							if(name.contains("grp_"), {
								hub.drones.groupDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.groupDict[key].selected = true;
									});
								});
							});	
							drones.selectedName = collectivename; // only the name of the group/sat/chord
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
								oldfoundFlag = foundFlag;
								if(drone.name.asString.contains("_"), {
									if(drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename, {
										foundFlag = true;
									},{
										foundFlag = false;
									});
								}, {
									foundFlag = false;
								});	
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});						
						
						/*
						// select another drone
						var name, collectivename;
						var foundFlag = false;
						var oldfoundFlag = false;
						
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						selected = (selected+1).clip(0, drones.droneArray.size-1);

						name = drones.droneArray[selected].name.asString;
						[\oooo_, drones.droneArray[selected].name].postln;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						
						if(name.asString.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							[\collectivename, collectivename].postln;
							if(name.contains("chd_"), {
								hub.drones.chordDict[collectivename.asSymbol].selected = true;
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict[collectivename.asSymbol].selected = true;
							});	
							if(name.contains("grp_"), {
								hub.drones.groupDict[collectivename.asSymbol].selected = true;
							});	
							drones.selectedName = collectivename; // only the name of the group
//							drones.chordDict[name.asString[0..6].asSymbol].selected = true;
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
								"prisdfasd".scramble.postln;
								oldfoundFlag = foundFlag;
								if(drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename, {
									//drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});
						*/
						
						/*
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.asString.contains("_"), { // it's a group of some sort
							hub.drones.chordDict[name.asString[0..6].asSymbol].selected = true;
							selected = (selected + hub.drones.chordDict[name.asString[0..6].asSymbol].dronearray.size-1).clip(0, drones.droneArray.size-1);
						//}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
						//	drones.droneArray[selected].selected = true;
						//	drones.selected = selected;
						});
						
						*/
																		drones.selected = selected;
						selectedName = drones.droneArray[selected].name.asString;
						hub.postDroneState(selectedName, selected);
												
	//drones.droneArray[selected].selected = true;
						[\SELECTED_, drones.droneArray[selected].name].postln;
				
//						Document.listener.string = ""; // clear post window
//						string = "~"++selectedName++"\n"++
//						"~"++selectedName++".type = \\"++drones.droneArray[selected].type++"\n"++
//						"~"++selectedName++".tonic = "++drones.droneArray[selected].tonic++"\n"++
//						"~"++selectedName++".harmonics = "++drones.droneArray[selected].harmonics++"\n"++
//						"~"++selectedName++".amp = "++drones.droneArray[selected].amp++"\n"++
//						"~"++selectedName++".speed = "++(drones.droneArray[selected].speed*1000)++"\n"++
//						"~"++selectedName++".length = "++(drones.droneArray[selected].length*360/(2*pi))++"\n"++
//						"~"++selectedName++".angle = "++(drones.droneArray[selected].angle*360/(2*pi))++"\n"++
//						"~"++selectedName++".degree = "++drones.droneArray[selected].degree++"\n"++
//						"~"++selectedName++".ratio = "++drones.droneArray[selected].ratio++"\n"++
//						"~"++selectedName++".env = "++drones.droneArray[selected].env++"\n"++
//						"~"++selectedName++".octave = "++drones.droneArray[selected].octave++"\n";
//						Document.listener.string = string; // add info
//						if(hub.post, { hub.interpreter.postview.string_(string) });

						nil;
					});

/*

					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						// select another drone
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						[\oooo_, drones.droneArray[selected].name].postln;
						
						selected = (selected+1).clip(0, drones.droneArray.size-1);
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(drones.droneArray[selected].name.asString.contains("_"), { // it's a group of some sort
							
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
								"prisdfasd".scramble.postln;
								oldfoundFlag = foundFlag;
								if(drones.droneArray[selected].name.asString[0..6] == drone.name.asString[0..6], {
									drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.droneArray[selected].selected = true;
						});
													drones.selected = selected;
//	drones.droneArray[selected].selected = true;

						nil;
					});

					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						// select another drone
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						[\oooo_, drones.droneArray[selected].name].postln;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(drones.droneArray[selected].name.asString.contains("_"), { // it's a group of some sort
							
							block{ arg break;
							drones.droneArray.do({ arg drone, i;
								oldfoundFlag = foundFlag;
								if(drones.droneArray[selected].name.asString[0..6] == drone.name.asString[0..6], {
									drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
									selected = i.clip(0, drones.droneArray.size-1);
									("selected = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.droneArray[selected].selected = true;
						});
													drones.selected = selected;
//	drones.droneArray[selected].selected = true;

						nil;
					});

*/

					if(  (mod & 524288 == 524288)  && (unicode==127), { // cmd + backdelete button = KILL Selected drone from textview
						drones.droneArray[selected].kill;
					});

					if((mod == 131332) || (mod == 131076) && (keycode==36), { // evaluate code in SC mode -> (SHIFT + RETURN)
//						Document.listener.string_("");
//						{
//						postview.string_(Document.listener.string);
//						// postview.select(postview.string.size-2,0); // THIS CAN WORK in Qt
//				//		if(dev.not, {Document.listener.string = ""}); // XXX REVISE !!!
//						}.defer(0.05);

					});

				});

		// 9. POSTVIEW (Output Log)
		postview = TextView.new(mainwin, postrect)
					.palette_(darkPalette)
					.focusColor_(Color.clear)
					.background_(Color.black)
					.stringColor_(Color.green) 
					.hasVerticalScroller_(true)
					.font_(Font("Helvetica", 12 ))
					.resize_(2)
					.string_("lorenzoantei.com");

		// 10. Linea Divisoria
		lineview = UserView.new(mainwin, Rect(postrect.left, postrect.top-2, postrect.width, 4))
            .background_(Color.green.alpha_(0.5));
	}


	
	opInterpreter { "WARNING: THIS METHOD IS NOT USED ANYMORE".postln; }
    
    // ... METODI OPINTERPRETER E ALTRI (Mantienili come sono, non servono alla grafica) ...
    // Per brevità ho tagliato il resto, ma tu lascia i metodi opInterpreter, gui, codescore, ecc.
    // L'unica cosa che conta è il metodo initDroneInterpreter sopra.

	gui { | bool |
		if( bool, {
			textview.bounds_(Rect(rect.left, 400, rect.width, rect.height-400));
		}, {
			textview.bounds_(rect);
		});
		mainwin.refresh;
	}

	codescore { | bool |
		if( bool, {
			textview.bounds_(Rect(rect.left, 703, rect.width, rect.height-300));
			postview.bounds_(Rect(rect.left, 1100, rect.width, rect.height)); 
			lineview.bounds_(Rect(rect.left, 703-2, rect.width, 4));
		}, {
			"removing codescore".postln;
			lineview.bounds_(Rect(postrect.left, postrect.top-2, postrect.width, 4));
			textview.bounds_(rect);
			postview.bounds_(postrect);
		});
		mainwin.refresh;
	}

	newSelect { |argselectname|
		textview.string_(textview.string++"\n> "++argselectname);
	}

	getTextViewString {
		^textview.string;
	}

	setTextViewString_ {arg string;
		textview.string_("\n\n"++string++"\n");
	}
}
