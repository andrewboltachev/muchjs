muchjs
======

Very experimental JavaScript/JSX to ClojureScript converter.

Uses:

* babel as JavaScript parser
* custom dependency: regexpforobj — library for parsing


Example
=======
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
