//variables
//main diagram object
var classDiagram;
var generatorURL = 'http://m-lab.etf.unibl.org:8080/generator/services/generate/cdmd';//TODO: update
var layoutServiceURL = 'http://m-lab.etf.unibl.org:8080/amadeos_umlcdlayouter/services/layout/json';
//helper toolbar variables
var position = { x: 0, y: 0 }; //current mouse click position - place to add new class by default
//selected classes for new association
var selectedClassA;
var selectedClassB;
//function that will be executed from toolbar. Used to simplify function calls
var handlerFunction = null;
//helper variable to indicate user intention
var addRelation = false;
//canvas container
var container;

var _canvasWidth = 1100;
var _canvasHeight = 500;

function initAll() {
	initilizeCanvas();
    initModel(); //function from model.js
    initDiagram();
}

//functions
//set default values
function initDiagram() {
    //classDiagram= null;
    classDiagram = new UMLClassDiagram({
        id: 'div1',
        width: _canvasWidth,
        height: _canvasHeight
    });
    classDiagram.setId("class_diagram");
    selectedClassA = null;
    selectedClassB = null;
    position = { x: 0, y: 0 };
    handlerFunction = null;
    addRelation = false;
    classDiagram.draw();


    //override native function to prevent ID overwrite
    classDiagram._enumerateElements = function () {
        var t, e = 0, i = 0; for (t = 0; t < this._nodes.length; t++)
            if (t > i && (i = t), this._nodes[t]._id = (this._nodes[t]._id || i),
                this._nodes[t] instanceof SuperNode) {
                    for (e = i + 1; e < this._nodes[t]._nodeChilds.length + i + 1; e++)
                        this._nodes[t]._nodeChilds[e - i - 1]._id = (this._nodes[t]._nodeChilds[e - i - 1]._id || e); i = e
            } else t != i && (i += 1); for (t = 0; t < this._relations.length; t++)this._relations[t]._id = (this._relations[t]._id || t)
    };
}

//init UI component
function initilizeCanvas() {
    container = document.getElementById('div1');
    var width = _canvasWidth;
    var height = _canvasHeight;
    container.setAttribute('class', 'ud_diagram_div');
    container.style.width = width + 'px';
    container.style.height = height + 'px';

    //remove all canvas elements before draw new   
    var allCleared = false;
    while (allCleared == false) {
        var canvasElements = document.getElementsByClassName('ud_diagram_canvas');
        for (var i = 0; i < canvasElements.length; i++) {
            try {
                canvasElements[i].parentNode.removeChild(canvasElements[i]);
            } catch (err) {
            }
        }
        allCleared = document.getElementsByClassName('ud_diagram_canvas').length == 0;
    }

    var canvas = document.createElement('canvas');
    canvas.setAttribute('class', 'ud_diagram_canvas');
    canvas.width = width;
    canvas.height = height;
    var mainContext = canvas.getContext('2d');
    container.appendChild(canvas);

    canvas.onmousedown = function () {
        return false;
    }
    var motionContext = canvas.getContext('2d');

    initDiagramClickEvent();
}

function initDiagramClickEvent() {
    container.addEventListener('click', function (event) {
        position.x = event.layerX;
        position.y = event.layerY;

        var element = classDiagram.getElementByPoint(position.x, position.y);
        if (element && element._type == "UMLClass") {
            if (!addRelation) {
                selectedClassA = element;
                selectedClassB = null;
            } else {
                selectedClassB = element;
                addRelation = false;
                if (handlerFunction && selectedClassA && selectedClassB) {
                    handlerFunction();
                }
            }
        } else {
            selectedClassB = null;
            selectedClassA = null;
        }
    }, false);
}

function addClass() {
    var umlClassNode = new UMLClass({
        x: position.x,
        y: position.y
    });
    umlClassNode.setValue('name', "New Class");
    classDiagram.addElement(umlClassNode);
    classDiagram.draw();
    classDiagram.interaction(true);
}

function deleteElement() {
    var element = classDiagram.getElementByPoint(position.x, position.y);
    if (element && element instanceof Element) {
        classDiagram.delElement(element);
        classDiagram.draw();
    }
}

function clearAll() {
    initAll();
}

function addGeneralization() {
    var gen = new UMLGeneralization({
        a: selectedClassA,
        b: selectedClassB
    });
    classDiagram.addElement(gen);
    selectedClassB = null;
    classDiagram.draw();
}

function addComposition() {
    var gen = new UMLComposition({
        a: selectedClassA,
        b: selectedClassB
    });
    classDiagram.addElement(gen);
    selectedClassB = null;
    classDiagram.draw();
}

function addAssociation() {
    var gen = new UMLAssociation({
        a: selectedClassA,
        b: selectedClassB
    });
    classDiagram.addElement(gen);
    selectedClassB = null;
    classDiagram.draw();
}

function addAggregation() {
    var gen = new UMLAggregation({
        a: selectedClassA,
        b: selectedClassB
    });
    classDiagram.addElement(gen);
    selectedClassB = null;
    classDiagram.draw();
}

function addRelationEvent(handler) {
    handlerFunction = handler;
    addRelation = true;
}

//set model submit event
function initSubmitEvent() {
    // generatorForm
    var generatorForm = document.forms.namedItem("generatorForm");
    generatorForm.addEventListener('submit', function (event) {
        initAll();

        var fd = new FormData(generatorForm);

        var xhttp = new XMLHttpRequest();
        // Define what happens on successful data submission
        xhttp.addEventListener('load', function (event1) {

            var response = JSON.parse(xhttp.responseText);
            readUML(response['uml'], '');
            readNotation(response['notation']);
        });

        // Define what happens in case of error
        xhttp.addEventListener('error', function (event1, err) {
            console.log(event1, err)
            document.getElementById('div1').innerHTML = 'Status: something went wrong';
        });

        xhttp.open('POST', generatorURL, true);

        xhttp.send(fd);
        event.preventDefault();
    }, false);
    
    // xmiForm
	var xmiForm = document.forms.namedItem("xmiForm");
    xmiForm.addEventListener('submit', function (event) {
        initAll();

        var fd = new FormData(xmiForm);

        var xhttp = new XMLHttpRequest();
        // Define what happens on successful data submission
        xhttp.addEventListener('load', function (event1) {

            var response = JSON.parse(xhttp.responseText);
            readUML(response['uml'], '');
            readNotation(response['notation']);
        });

        // Define what happens in case of error
        xhttp.addEventListener('error', function (event1, err) {
            console.log(event1, err)
            document.getElementById('div1').innerHTML = 'Status: something went wrong';
        });

        xhttp.open('POST', layoutServiceURL, true);

        xhttp.send(fd);
        event.preventDefault();
    }, false);
}
