var CodeMirrorObj = function() {};
CodeMirrorObj.prototype.state = {
    "completionActive": function() {}
};
CodeMirrorObj.prototype.getValue = function(){};
CodeMirrorObj.prototype.setValue = function(value){};
CodeMirrorObj.prototype.on = function (event, handler) {};
CodeMirrorObj.prototype.off = function (event, handler) {};
CodeMirrorObj.prototype.getSelection = function(){};
CodeMirrorObj.prototype.replaceString = function(str){};
CodeMirrorObj.prototype.focus = function(){};
CodeMirrorObj.prototype.setOption = function(option, value){};
CodeMirrorObj.prototype.getOption = function(option){};
CodeMirrorObj.prototype.cursorCoords = function(start){};
CodeMirrorObj.prototype.charCoords = function(pos){};
CodeMirrorObj.prototype.coordsChar = function(obj){};
CodeMirrorObj.prototype.undo = function(){};
CodeMirrorObj.prototype.redo = function(){};
CodeMirrorObj.prototype.historySize = function(){};
CodeMirrorObj.prototype.indentLine = function(line, dir){};
CodeMirrorObj.prototype.getSearchCursor = function(query, start, caseFold){};
CodeMirrorObj.prototype.getTokenAt = function(pos){};
CodeMirrorObj.prototype.markText = function(from, to, className){};
CodeMirrorObj.prototype.setMarker = function(line, opt_text, opt_className){};
CodeMirrorObj.prototype.clearMarker = function(line){};
CodeMirrorObj.prototype.setLineClass = function(line, className){};
CodeMirrorObj.prototype.lineInfo = function(line){};
CodeMirrorObj.prototype.addWidget = function(pos, node, scrollIntoView){};
CodeMirrorObj.prototype.matchBrackets = function(){};
CodeMirrorObj.prototype.lineCount = function(){};
CodeMirrorObj.prototype.getCursor = function(start){};
CodeMirrorObj.prototype.somethingSelected = function(){};
CodeMirrorObj.prototype.setCursor = function(pos, opt_ch){};
CodeMirrorObj.prototype.setSelection = function(start, end){};
CodeMirrorObj.prototype.getLine = function(n){};
CodeMirrorObj.prototype.setLine = function(n, text){};
CodeMirrorObj.prototype.removeLine = function(n){};
CodeMirrorObj.prototype.getRange = function(from, to){};
CodeMirrorObj.prototype.replaceRange = function(str, from, to){};
CodeMirrorObj.prototype.coordsFromIndex = function(index){};
CodeMirrorObj.prototype.operation = function(func){};
CodeMirrorObj.prototype.refresh = function(){};
CodeMirrorObj.prototype.getInputField = function(){};
CodeMirrorObj.prototype.getWrapperElement = function(){};
CodeMirrorObj.prototype.getScrollerElement = function(){};
CodeMirrorObj.prototype.getGutterElement = function(){};
CodeMirrorObj.prototype.getStateAfter = function(opt_line){};

var CodeMirrorFromTextAreaObj = function(){};
CodeMirrorFromTextAreaObj.prototype.save = function(){};
CodeMirrorFromTextAreaObj.prototype.toTextArea = function(){};

var CodeMirrorLineHandle = function(){};

var CodeMirrorCursor = function(){};
CodeMirrorCursor.prototype.findNext = function(){};
CodeMirrorCursor.prototype.findPrevious = function(){};
CodeMirrorCursor.prototype.from = function(){};
CodeMirrorCursor.prototype.to = function(){};
CodeMirrorCursor.prototype.replace = function(text){};

var CodeMirror = function(element, opt_options) {};
CodeMirror.fromTextArea = function(textAreaElement) {};
CodeMirror.defineExtension = function(name, value){};
CodeMirror.commands = {
    "autocomplete": function(editor, something, obj) {}
};
