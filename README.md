# muchjs


Very experimental JavaScript/JSX to ClojureScript converter.

Uses:

* [babel](https://babeljs.io/) as JavaScript parser
* custom dependency: [RegExpForObj](https://github.com/andrewboltachev/regexpforobj) — library for parsing


## Example (taken from awesome [ReactDND](http://gaearon.github.io/react-dnd/) project):

```jsx
// Let's make <Card text='Write the docs' /> draggable!

var React = require('react');
var DragSource = require('react-dnd').DragSource;
var ItemTypes = require('./Constants').ItemTypes;
var PropTypes = React.PropTypes;

/**
 * Implements the drag source contract.
 */
var cardSource = {
  beginDrag: function (props) {
    return {
      text: props.text
    };
  }
}

/**
 * Specifies the props to inject into your component.
 */
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}

var Card = React.createClass({
  propTypes: {
    text: PropTypes.string.isRequired,

    // Injected by React DnD:
    isDragging: PropTypes.bool.isRequired,
    connectDragSource: PropTypes.func.isRequired
  },

  render: function () {
    var isDragging = this.props.isDragging;
    var connectDragSource = this.props.connectDragSource;
    var text = this.props.text;

    return connectDragSource(
      <div style={{ opacity: isDragging ? 0.5 : 1 }}>
        {text}
      </div>
    );
  }
});

// Export the wrapped component:
module.exports = DragSource(ItemTypes.CARD, cardSource, collect)(Card);
```

Result:
```clojure
(def React (require "react"))


(def DragSource (.-DragSource (require "react-dnd")))


(def ItemTypes (.-ItemTypes (require "./Constants")))


(def PropTypes (.-PropTypes React))


(def
 cardSource 
 #js {:beginDrag (fn [props] #js {:text (.-text props)})})


(defn
 collect
 [connect monitor]
 #js {:connectDragSource (.dragSource connect), :isDragging (.isDragging monitor)})


(def
 Card
 (.createClass
  React
  #js {:propTypes #js {:text (.-isRequired (.-string PropTypes)), :isDragging (.-isRequired (.-bool PropTypes)), :connectDragSource (.-isRequired (.-func PropTypes))}, :render (fn [] (let [isDragging (.-isDragging (.-props this)) connectDragSource (.-connectDragSource (.-props this)) text (.-text (.-props this))] (connectDragSource [:div {:style #js {:opacity (if isDragging 0.5 1)}} "\n        " text "\n      "])))}))


(set!
 (.-exports module)
 ((DragSource (.-CARD ItemTypes) cardSource collect) Card))
```


## Running locally
```
lein deps
npm install
rlwrap lein figwheel
# ...on other window:
node_modules/babel-cli/bin/babel-node.js target/server_dev/muchjs.js example.js
```


## License
MIT License

Copyright (c) 2016 Andrei Boltachev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
