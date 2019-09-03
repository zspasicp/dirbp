

var contents = "", parser, xmlDoc;
var umlClasses = []; // jsuml2 objects representing UML classes
var classes = []; // javaScript objects representing UML classes
var associations = []; // javaScript objects representing UML associations
var databaseName = "";
var mapDataTypes = new Map();
var mapAttributes = new Map();
var mapClasses = new Map();


function initModel() {
	contents = "", parser = "", xmlDoc = "";
	umlClasses = [];
	classes = [];
	associations = [];
	
}

function readNotationFile(e) {
	var file = e.target.files[0];
	if (!file) {
		return;
	}
	
	var reader = new FileReader();
	reader.onload = function (e) {
		var contents = e.target.result;
		//displayContents(contents);
		parser = new DOMParser();
		xmlDoc = parser.parseFromString(contents, "text/xml");
		var children = xmlDoc.firstChild.childNodes;
		var attrs = [];
		for (var i = 0; i < children.length; i++) {
			// console.log(children[i]);
			// Shape node representing UML Class node
			if (children[i].tagName == 'children' && children[i].attributes['xmi:type'] && children[i].attributes['xmi:type'].textContent == 'notation:Shape') {
				var elementNodes = children[i].getElementsByTagName("element");
				var classNode;
				for (var j = 0; j < elementNodes.length; j++) {
					if (elementNodes[j].attributes['xmi:type'] && elementNodes[j].attributes['xmi:type'].textContent == 'uml:Class') {
						classNode = elementNodes[j];
						break;
					}
				}
				//console.log(classNode);
				if (classNode.attributes['xmi:type'] && classNode.attributes['xmi:type'].textContent == 'uml:Class') {
					// class id
					var classId = classNode.attributes['href'].textContent;
					classId = classId.substr(classId.lastIndexOf("#") + 1);
					var activeClassObject = findClassObjectById(classId);
					if (activeClassObject) {
						// adding coordinates to class objects
						var layoutConstraintNodes = children[i].getElementsByTagName("layoutConstraint");
						// layoutConstraint which belongs to the uml:Class node is always the last one in notation:Shape node?
						var layoutConstraintNodeClass = layoutConstraintNodes[layoutConstraintNodes.length - 1];
						if (layoutConstraintNodeClass && layoutConstraintNodeClass.attributes['x'] && layoutConstraintNodeClass.attributes['y']) {
							activeClassObject.x = parseInt(layoutConstraintNodeClass.attributes['x'].textContent);
							activeClassObject.y = parseInt(layoutConstraintNodeClass.attributes['y'].textContent);
						}
					}
				} else { }
			} else if (children[i].tagName == 'edges') { // Edges node representing associations points
				var bendpointsNode = children[i].getElementsByTagName("bendpoints"),
					assNode = children[i].getElementsByTagName("element")[0],
					sourceClassId = children[i].attributes['source'].textContent,
					targetClassId = children[i].attributes['target'].textContent;
				var assId = assNode.attributes['href'].textContent;
				assId = assId.substr(assId.lastIndexOf("#") + 1);
				var assObject = associations.find(obj => obj.id === assId);
				if (bendpointsNode && bendpointsNode[0] && assObject) {
					assObject.points = [];
					var points = bendpointsNode[0].attributes['points'].textContent.split('$');
					for (var k = 0; k < points.length; k++) {
						var arr = JSON.parse(points[k]);
						var posX = arr[0],
							posY = arr[1];
						assObject.points.push({
							x: posX,
							y: posY
						});
					}
				}
			}
		}

		drawUMLClassDiagram();
	};
	reader.readAsText(file);
}

// for server side
function readNotation(notation) {
	
	var contents = notation;
	//displayContents(contents);
	parser = new DOMParser();
	xmlDoc = parser.parseFromString(contents, "text/xml");
	var children = xmlDoc.firstChild.childNodes;
	var attrs = [];
	for (var i = 0; i < children.length; i++) {
	//	 console.log(children[i]);
		// Shape node representing UML Class node
		if (children[i].tagName == 'children' && children[i].attributes['xmi:type'] && children[i].attributes['xmi:type'].textContent == 'notation:Shape') {
			var elementNodes = children[i].getElementsByTagName("element");
			var classNode;
			for (var j = 0; j < elementNodes.length; j++) {
				if (elementNodes[j].attributes['xmi:type'] && elementNodes[j].attributes['xmi:type'].textContent == 'uml:Class') {
					classNode = elementNodes[j];
					break;
				}
			}
			//console.log(classNode);
			if (classNode.attributes['xmi:type'] && classNode.attributes['xmi:type'].textContent == 'uml:Class') {
				// class id
				var classId = classNode.attributes['href'].textContent;
				classId = classId.substr(classId.lastIndexOf("#") + 1);
				var activeClassObject = findClassObjectById(classId);
				if (activeClassObject) {
					// adding coordinates to class objects
					var layoutConstraintNodes = children[i].getElementsByTagName("layoutConstraint");
					// layoutConstraint which belongs to the uml:Class node is always the last one in notation:Shape node?
					var layoutConstraintNodeClass = layoutConstraintNodes[layoutConstraintNodes.length - 1];
					if (layoutConstraintNodeClass && layoutConstraintNodeClass.attributes['x'] && layoutConstraintNodeClass.attributes['y']) {
						activeClassObject.x = parseInt(layoutConstraintNodeClass.attributes['x'].textContent);
						activeClassObject.y = parseInt(layoutConstraintNodeClass.attributes['y'].textContent);
					}
				}
			} else { }
		} else if (children[i].tagName == 'edges') { // Edges node representing associations points
			var bendpointsNode = children[i].getElementsByTagName("bendpoints"),
				assNode = children[i].getElementsByTagName("element")[0],
				sourceClassId = children[i].attributes['source'].textContent,
				targetClassId = children[i].attributes['target'].textContent;
			var assId = assNode.attributes['href'].textContent;
			assId = assId.substr(assId.lastIndexOf("#") + 1);
			var assObject = associations.find(obj => obj.id === assId);
			if (bendpointsNode && bendpointsNode[0] && assObject) {
				assObject.points = [];
				var points = bendpointsNode[0].attributes['points'].textContent.split('$');
				for (var k = 0; k < points.length; k++) {
					var arr = JSON.parse(points[k]);
					var posX = arr[0],
						posY = arr[1];
					assObject.points.push({
						x: posX,
						y: posY
					});
				}
			}
		}
	}
	drawUMLClassDiagram();
}

// drawing of earlier prepared classes and associations
function drawUMLClassDiagram() {
	
	for (var i = 0; i < classes.length; i++) {
		var umlClassNode = new UMLClass({
			x: classes[i].x,
			y: classes[i].y
		});
		
		umlClassNode.setValue('name', classes[i].name);
		umlClassNode.setId(classes[i].id)
		umlClassNode._width = 100;
		umlClassNode._beforeWidth = 100;
		umlClassNode._height = 100;
		umlClassNode._beforeHeight = 100;
		
		if(classes[i].type == 'View') {
			umlClassNode.setBackgroundColor('#ffaadd');
		}
		// adding attributes
		for (var j = 0; j < classes[i].attributes.length; j++)
			if (!classes[i].attributes[j].association) { // attributes representing association are not displayed
				if (classes[i].attributes[j].type) {
					var tipPodatka = mapDataTypes.get(classes[i].attributes[j].type);
					umlClassNode.addValue('attributes', classes[i].attributes[j].name + ':' + tipPodatka);
					
				}
				else
					umlClassNode.addAttribute(classes[i].attributes[j].name);
			}

		// adding operations
		for (var j = 0; j < classes[i].operations.length; j++) {			
			var operString = classes[i].operations[j].name + "(";			
			for (var z = 0; z < classes[i].operations[j].params.length; z++) {
				var dataTypeOperation = mapDataTypes.get(classes[i].operations[j].params[z].attributes['type'].textContent);
				operString += classes[i].operations[j].params[z].attributes['name'].textContent + ":" + dataTypeOperation;
				if (z < classes[i].operations[j].params.length - 1)
					operString += ",";
			}
			operString += ")";
			umlClassNode.addValue('operations', operString);
			
		}

		classDiagram.addElement(umlClassNode);
		umlClasses.push(umlClassNode);
		classes[i].umlClassNode = umlClassNode;
	}

	// check for Specialization/Generalization
	for (var i = 0; i < classes.length; i++) {
		// this class is specialization
		if (classes[i].generalization && classes[i].generalization.general) {
			var generalizationClass = findClassObjectById(classes[i].generalization.general);
			if (generalizationClass) {
				var gen = new UMLGeneralization({
					b: generalizationClass.umlClassNode,
					a: classes[i].umlClassNode
				});
				classDiagram.addElement(gen);
			}
		}
	}
	
	var multiAXArray = [];
	var multiAYArray = [];
	var multiBXArray = [];
	var multiBYArray = [];
	var assocNamesXArray = [];
	var assocNamesYArray = [];

	// adding associations
	for (var i = 0; i < associations.length; i++) {
		var association1;
		var reverse = false;
		if (associations[i].aggregation != null) {
			if (associations[i].aggregation == 0) {
				association1 = new UMLAggregation({
					b: associations[i].ownedClassObjects[0].umlClassNode,
					a: associations[i].ownedClassObjects[1].umlClassNode
				});
				reverse = true;
			} else
				association1 = new UMLAggregation({
					b: associations[i].ownedClassObjects[1].umlClassNode,
					a: associations[i].ownedClassObjects[0].umlClassNode
				});
		} else if (associations[i].composition != null) {
			if (associations[i].composition == 0) {
				association1 = new UMLComposition({
					b: associations[i].ownedClassObjects[0].umlClassNode,
					a: associations[i].ownedClassObjects[1].umlClassNode
				});
				reverse = true;
			} else
				association1 = new UMLComposition({
					b: associations[i].ownedClassObjects[1].umlClassNode,
					a: associations[i].ownedClassObjects[0].umlClassNode
				});
		} else if (associations[i].dependency != null) {
			association1 = new UMLDependency({
				b: associations[i].ownedClassObjects[1].umlClassNode,
				a: associations[i].ownedClassObjects[0].umlClassNode
			});
		} else {
			association1 = new UMLAssociation({
				b: associations[i].ownedClassObjects[1].umlClassNode,
				a: associations[i].ownedClassObjects[0].umlClassNode
			});
		}

		association1.setName(associations[i].name);
		association1.setId(associations[i].id);
		
		// intermediate points
		association1._points = [];
		// TEMP - TODO
		//var zadnja = association1._points.pop();
		for(var j=0; j<(associations[i].points ? associations[i].points.length : 0); j++) {
			association1._points.push(new Point(associations[i].points[j].x,associations[i].points[j].y));
		}
		// END of TEMP

		if (!(association1 instanceof Dependency)) {
			// Possible cardinalities: 1, 0...1, 1..*, *, m
			// setMultiplicityA
			var multiplicity1,
				multiplicity2;
			var association0Lower = associations[i].multiplicities[0].lowerValue.value;
			var association1Lower = associations[i].multiplicities[1].lowerValue.value;
			var association0Upper = associations[i].multiplicities[0].upperValue.value;
			var association1Upper = associations[i].multiplicities[1].upperValue.value;

			if ((!association0Lower || association0Lower == '0') && association0Upper === '1') // 0...1
				multiplicity1 = '0..1';
			else if (association0Lower && association0Lower === '1'
				&& association0Upper === '1') // 1
				multiplicity1 = '1';
			else if ((!association0Lower || association0Lower == '0') && association0Upper === '*') // *
				multiplicity1 = '*';
			else if (association0Lower && association0Lower === '1'
				&& association0Upper === '*') // 1..*
				multiplicity1 = '1..*';
			else if ((!association0Lower || association0Lower == '0') && association0Upper) // m
				multiplicity1 = association0Upper;
			else {
				// throw new Exception ('Unknown.')
			}

			// setMultiplicityB
			if ((!association1Lower || association1Lower == '0') && association1Upper === '1') // 0..1
				multiplicity2 = '0..1';
			else if (association1Lower && association1Lower === '1'
				&& association1Upper === '1') // 1
				multiplicity2 = '1';
			else if ((!association1Lower || association1Lower == '0') && association1Upper === '*') // *
				multiplicity2 = '*';
			else if (association1Lower && association1Lower === '1'
				&& association1Upper === '*') // 1..*
				multiplicity2 = '1..*';
			else if ((!association1Lower || association1Lower == '0') && association1Upper) // m
				multiplicity2 = association1Upper;
			else {
				// throw new Exception ('Unknown.')
			}

			if (reverse) {
				association1.setMultiplicityA(multiplicity2);
				association1.setMultiplicityB(multiplicity1);
			} else {
				association1.setMultiplicityA(multiplicity1);
				association1.setMultiplicityB(multiplicity2);
			}
		}
		
		// check if left multiplicities of two associations are overlapped
		if (association1._multiA) {
		var counter = checkOverlapingMultiplicities(multiAXArray, multiAYArray, association1._multiA._x, association1._multiA._y);
		if(counter){
			multiAXArray.push(association1._multiA._x);
			multiAYArray.push(association1._multiA._y);
			var x = counter*10;
			association1._multiA._x += x;
			association1._multiA._y += x;
		} else{
			multiAXArray.push(association1._multiA._x);
			multiAYArray.push(association1._multiA._y);
		}
		}
		
		// check if right multiplicities of two associations are overlapped
		if (association1._multiB) {
		var counter = checkOverlapingMultiplicities(multiBXArray, multiBYArray, association1._multiB._x, association1._multiB._y);
		if(counter){
			multiBXArray.push(association1._multiB._x);
			multiBYArray.push(association1._multiB._y);
			var x = counter*10;
			association1._multiB._x += x;
			association1._multiB._y += x;
		} else{
			multiBXArray.push(association1._multiB._x);
			multiBYArray.push(association1._multiB._y);
		}
		}
		
		// check if names of two associations are overlapped
		if (association1._name) {
		var counter = checkOverlapingNames(assocNamesXArray, assocNamesYArray, association1._name._x, association1._name._y);
		if(counter){
			assocNamesXArray.push(association1._name._x);
			assocNamesYArray.push(association1._name._y);
			var x = counter*10;
			//association1._name._x += x;
			association1._name._y += x;
		} else{
			assocNamesXArray.push(association1._name._x);
			assocNamesYArray.push(association1._name._y);
		}
		}
		
		classDiagram.addElement(association1);

		var xx = new UMLAssociation();
		classDiagram.addRelationFromPoints(xx, 0, 0, 50, 50);
	}

	classDiagram.draw();
	classDiagram.interaction(true);

	// Update dynamically the canvas height
	classDiagram.setUpdateHeightCanvas(true);
}

function checkOverlapingMultiplicities(arrayX, arrayY, valX, valY){
	if(!arrayX || !valX || !arrayY || !valY) return false;
	var counter = 0;
	for (var i = 0; i < arrayX.length; i++){
		if(arrayX[i] === valX && arrayY[i] === valY)
			counter ++;
	}
	return counter;
}

function checkOverlapingNames(arrayX, arrayY, valX, valY){
	if(!arrayX || !valX || !arrayY || !valY) return false;
	var counter = 0;
	for (var i = 0; i < arrayX.length; i++){
		if(arrayX[i] === valX || arrayY[i] === valY)
			counter ++;
	}
	return counter;
}

function readUMLFile(e) {
	
	var file = e.target.files[0];
	if (!file) {
		return;
	}
	var reader = new FileReader();
	reader.onload = function (e) {
		var contents = e.target.result;
		//displayContents(contents);
		parser = new DOMParser();
		xmlDoc = parser.parseFromString(contents, "text/xml");
		var umlModelNode = xmlDoc.firstChild;
		if (umlModelNode.tagName == 'xmi:XMI') // little hack for .uml file begining with xmi:XMI element, instead of uml:Model
			for (var i = 0; i < umlModelNode.children.length; i++) {
				if (umlModelNode.children[i].tagName = 'uml:Model') {
					umlModelNode = umlModelNode.children[i];
					break;
				}
			}
		var children = umlModelNode.childNodes;
		for (var i = 0; i < children.length; i++) {
			//console.log(children[i]);
			if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Class') {
				var classObject = createUMLClassObject(children[i]);
				classes.push(classObject);
			} else if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Association') {
				var associationObject = createUMLAssociationObject(children[i]);
				associations.push(associationObject);
				//console.log("Association: ");
				//console.log(associationObject);
			} else if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Dependency') {
				var dependencyObject = createUMLDependencyObject(children[i]);
				associations.push(dependencyObject);
				//console.log("Association: ");
				//console.log(associationObject);
			} else if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Package') {
				var childrenP = children[i].childNodes;
				for (var j = 0; j < childrenP.length; j++) {
					if (childrenP[j].tagName == 'packagedElement' && childrenP[j].attributes['xmi:type'].textContent == 'uml:Class') {
						var classObject = createUMLClassObject(childrenP[j]);
						classes.push(classObject);
					} else if (childrenP[j].tagName == 'packagedElement' && childrenP[j].attributes['xmi:type'].textContent == 'uml:Association') {
						var associationObject = createUMLAssociationObject(childrenP[j]);
						associations.push(associationObject);
					} else if (childrenP[j].tagName == 'packagedElement' && childrenP[j].attributes['xmi:type'].textContent == 'uml:Dependency') {
						var dependencyObject = createUMLDependencyObject(childrenP[j]);
						associations.push(dependencyObject);
					}
				}

			}
		}
	};
	reader.readAsText(file);
}

// for server side
function readUML(uml, databaseName) {
	
	var contents = uml;
	//displayContents(contents);
	parser = new DOMParser();
	xmlDoc = parser.parseFromString(contents, "text/xml");
	
	var children;
	var types;
	var modelChildren = xmlDoc.getElementsByTagName('uml:Model')[0].childNodes;
	
	var size = modelChildren.length;
	
	for (var i = 0; i < size; i++) {
		
		if (modelChildren[i].tagName == 'packagedElement' &&
			modelChildren[i].attributes['xmi:type'].textContent == 'uml:Package' &&
			modelChildren[i].attributes['name'].textContent == databaseName
				) {
				children = modelChildren[i].childNodes;
				classDiagram.setName(modelChildren[i].attributes['name'].textContent); //this will set package name
				classDiagram.setId(modelChildren[i].attributes['xmi:id'].textContent); //this will set class id
			}
		
		if (modelChildren[i].tagName == 'packagedElement' &&
				modelChildren[i].attributes['xmi:type'].textContent == 'uml:Package' &&
				//modelChildren[i].attributes['name'].textContent == 'ICM_PT'
				modelChildren[i].attributes['name'].textContent == 'DataTypes'
					) {
					types = modelChildren[i].childNodes;
					//alert("idemo");
					for(var j = 0; j < types.length; j++) {
					if (types[j].tagName == 'packagedElement' && types[j].attributes['xmi:type'].textContent == 'uml:PrimitiveType') {
						//alert("ID: " + types[j].attributes['xmi:id'].textContent);
						//alert("namw " + types[j].attributes['name'].textContent);
						
							mapDataTypes.set(types[j].attributes['xmi:id'].textContent, types[j].attributes['name'].textContent)
						}
					}
				}		
		}	

	
	for (var i = 0; i < children.length; i++) {
				
		if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Class') {
			var classObject = createUMLClassObject(children[i]);
			classes.push(classObject);
		} 
	}
	
	for(var i = 0; i < children.length; i++) 
	{
		if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Association') {
			var associationObject = createUMLAssociationObject(children[i]);
			associations.push(associationObject);
		} else if (children[i].tagName == 'packagedElement' && children[i].attributes['xmi:type'].textContent == 'uml:Dependency') {
			var dependencyObject = createUMLDependencyObject(children[i]);
			associations.push(dependencyObject);
		}
	}
	
	
}


// creates object representing UML Class, from node packagedElement
function createUMLClassObject(classNode) {
	var classObject = new Object();
	classObject.id = classNode.attributes['xmi:id'].textContent;
	classObject.name = classNode.attributes['name'].textContent;
	classObject.type = classNode.attributes['type'].textContent;
	
	mapClasses.set(classNode.attributes['xmi:id'].textContent, classNode.attributes['name'].textContent);
		
	// attributes
	classObject.attributes = [];
	classObject.operations = [];
	for (var i = 0; i < classNode.childNodes.length; i++) {
		var classChild = classNode.childNodes[i];
		if (classChild.tagName == 'ownedAttribute' && classChild.attributes['xmi:type'].textContent == 'uml:Property') {
			var attrObject = createUMLAttributeObject(classChild);
			classObject.attributes.push(attrObject);
		} else if (classChild.tagName == 'ownedOperation' && classChild.attributes['xmi:type'].textContent == 'uml:Operation') {
			var operationObject = createUMLOperationObject(classChild);
			classObject.operations.push(operationObject);
		} else if (classChild.tagName == 'generalization' && classChild.attributes['xmi:type'].textContent == 'uml:Generalization') {
			var generalizationObject = createUMLGeneralizationObject(classChild);
			classObject.generalization = generalizationObject;
		}
	}
	return classObject;
}

// creates object representing UML Class operation, from node ownedOperation
function createUMLOperationObject(operationNode) {
	var operationObject = new Object();
	operationObject.id = operationNode.attributes['xmi:id'].textContent;
	if (operationNode.attributes['name'])
		operationObject.name = operationNode.attributes['name'].textContent;
	if (operationNode.attributes['visibility'])
		operationObject.visibility = operationNode.attributes['visibility'].textContent;
	//params
	var paramsNodes = operationNode.childNodes;
	if (paramsNodes) {
		operationObject.params = [];
		for (var i = 0; i < paramsNodes.length; i++) {
			if (paramsNodes[i] && paramsNodes[i].tagName == 'ownedParameter' && paramsNodes[i].attributes['name'] && paramsNodes[i].attributes['name'].textContent)
				operationObject.params.push(paramsNodes[i]);
			
		}
	}

	return operationObject;
}

// creates object representing UML Class Attribute, from node ownedAttribute
function createUMLAttributeObject(attrNode) {
	var attrObject = new Object();
	attrObject.id = attrNode.attributes['xmi:id'].textContent;
	if (attrNode.attributes['name'])
		attrObject.name = attrNode.attributes['name'].textContent;
	if (attrNode.attributes['visibility'])
		attrObject.visibility = attrNode.attributes['visibility'].textContent;
	if (attrNode.attributes['type']) { // this is a xmi:id of the uml:Class, ie. this attribute is a foreign key
		attrObject.type = attrNode.attributes['type'].textContent;
		//attrObject.name += "_fk"; // temporary
	}

	
	if (attrNode.attributes['association'])
		attrObject.association = attrNode.attributes['association'].textContent;

	attrObject.associations = {};

	if (attrNode.childNodes) {
		attrNode.childNodes.forEach((child) => {
			if (child.nodeName == 'type') {
				if (child.attributes['xmi:type'].textContent === 'uml:PrimitiveType') { // primitive types
					var primitiveType = child.attributes['href'].textContent;
					/*definisati tip podatka*/
										
					if (primitiveType.indexOf('Integer') !== -1)
						attrObject.type = 'Integer';
					else if (primitiveType.indexOf('String') !== -1)
						attrObject.type = 'String';
					else if (primitiveType.indexOf('Real') !== -1)
						attrObject.type = 'Real';
					else if (primitiveType.indexOf('Boolean') !== -1)
						attrObject.type = 'Boolean';
				}
			}
		});
	}
	//console.log(attrObject)
	return attrObject;
}

// creates object representing UML Dependency, from node packagedElement
function createUMLDependencyObject(dependencyNode) {
	var assObject = new Object();
	assObject.ownedClassObjects = [];
	assObject.id = dependencyNode.attributes['xmi:id'].textContent;
	if (dependencyNode.attributes['name'])
		assObject.name = dependencyNode.attributes['name'].textContent;
	var clientId = dependencyNode.attributes['client'].textContent;	
	var supplierId = dependencyNode.attributes['supplier'].textContent;
	
	var clientClassObject = findClassObjectById(clientId);
	var supplierClassObject = findClassObjectById(supplierId);
	
	if (clientClassObject) {
		assObject.ownedClassObjects[0] = clientClassObject; // class in association
	}
	if (supplierClassObject) {
		assObject.ownedClassObjects[1] = supplierClassObject; // class in association
	}
	assObject.dependency = 1;
	return assObject;
}

// creates object representing UML Generalization, from node packagedElement
function createUMLGeneralizationObject(node) {
	var assObject = new Object();
	assObject.ownedClassObjects = [];
	assObject.id = node.attributes['xmi:id'].textContent;
	assObject.general = node.attributes['general'].textContent;
	assObject.generalization = 1;
	return assObject;
}
// creates object representing UML Association, from node packagedElement
function createUMLAssociationObject(associationNode) {
	var assObject = new Object();
	assObject.id = associationNode.attributes['xmi:id'].textContent;
	if (associationNode.attributes['name'])
		assObject.name = associationNode.attributes['name'].textContent;
	assObject.memberEnd = associationNode.attributes['memberEnd'].textContent;

	assObject.multiplicities = [];
	assObject.ownedClassObjects = [];
	var counter = 0;
	for (var j = 0; j < associationNode.childNodes.length; j++) {
		if (associationNode.childNodes[j].tagName == 'ownedEnd') {
			var rownedEndNode = associationNode.childNodes[j];
			var ownedClassObject = findClassObjectById(rownedEndNode.attributes['type'].textContent);
			if (ownedClassObject) {
				assObject.ownedClassObjects[counter] = ownedClassObject; // class in association

				// Association, Composition
				if (rownedEndNode.attributes['aggregation'] && rownedEndNode.attributes['aggregation'].textContent) {
					if (rownedEndNode.attributes['aggregation'].textContent == 'shared') {
						assObject.aggregation = counter;
					} else if (rownedEndNode.attributes['aggregation'].textContent == 'composite') {
						assObject.composition = counter;
					}
				}

				if (rownedEndNode && rownedEndNode.attributes['xmi:type'].textContent == 'uml:Property' && rownedEndNode.attributes['type']) {
					if (rownedEndNode.childNodes) {
						rownedEndNode.childNodes.forEach((child) => {
							if (child.nodeName == 'lowerValue') {
								var attributes = child.attributes;
								assObject.multiplicities[counter] = {};
								assObject.multiplicities[counter]['lowerValue'] = {
									id: child.attributes.getNamedItem('xmi:id').value,
									type: child.attributes.getNamedItem('xmi:type').value,
									value: child.attributes.getNamedItem('value') ? child.attributes.getNamedItem('value').value : null
								};
							} else if (child.localName == 'upperValue') {
								var attributes = child.attributes;
								assObject.multiplicities[counter]['upperValue'] = {
									id: child.attributes.getNamedItem('xmi:id').value,
									type: child.attributes.getNamedItem('xmi:type').value,
									value: child.attributes.getNamedItem('value') ? child.attributes.getNamedItem('value').value : null
								};
							}
						});
					}
				}
			}
			counter++;
		}
	}
	return assObject;
}



// helper
function findClassObjectById(classId) {
	for (var i = 0; i < classes.length; i++)
		if (classes[i].id == classId)
			return classes[i];
	return null;
}
// helper
function findAttributeOfClassObjectById(classObject, attrId) {
	for (var i = 0; i < classObject.attributes.length; i++)
		if (classObject.attributes[i].association && classObject.attributes[i].association == attrId)
			return classObject.attributes[i];
	return null;
}
// helper
function displayContents(contents) {
	var element = document.getElementById('file-content');
	element.textContent = contents;
}

function exportDiagram() {

	var objectUrl = window.URL.createObjectURL(new Blob([classDiagram.getXML().outerHTML], {
		type: 'text/xml'
	}));
	var save = document.createElement('a');
	save.href = objectUrl;
	save.target = '_blank';
	save.download = "Export.xml";
	let event = document.createEvent('MouseEvents');
	event.initMouseEvent(
		"click", true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
	save.dispatchEvent(event);
}

function exportDiagramAsXMI() {
	
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var objectUrl = window.URL.createObjectURL(new Blob([this.responseText], {
			type: 'text/xml'
			}));
		var save = document.createElement('a');
		save.href = objectUrl;
		save.target = '_blank';
		save.download = "Export.uml";
		let event = document.createEvent('MouseEvents');
		event.initMouseEvent(
			"click", true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
		save.dispatchEvent(event);
		}
	};
	
	xhttp.open("POST", "http://m-lab.etf.unibl.org:8080/amadeos_xml2xmi/services/convert", true);
	xhttp.send(classDiagram.getXML().outerHTML);
		
}