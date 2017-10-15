(ns reagent-query.core-test
  (:require [cljs.test :refer-macros [is testing]]
            [devcards.core :refer-macros [deftest]]
            [reagent-query.core :as rq])
  (:require-macros [reagent-query.tests :refer [fact]]))

[[:chapter {:title "Introduction"}]]
"[Reagent](http://reagent-project.github.io) is a ClojureScript library that wraps [React](https://reactjs.org), and makes it even more awesome by leveraging the power of
Clojure's functional programming."

"Reagent allows the client-side of a web application to consist of nothing but pure ClojureScript functions that map the _state_, stored in special atoms,
to a [hiccup](https://github.com/weavejester/hiccup)-like representation of HTML.
To make the application interactive, event handlers such as `on-click` or `on-change` can be assigned ClojureScript functions (closures), 
which will be executed once the event fires."

[[:section {:title "The Problem"}]]
"Previous to ClojureScript, my last client-side experience involved [AngularJS](https://angularjs.org) (1.X).
Although React (and Reagent) present an overall better programming model than Angular (IMHO), one thing is missing in React/Reagent, 
and that's a coherent approach for unit-testing an application."

"This was rather surprising and disapointing.
After all, the code is reduced to pure ClojureScript functions, so why can't I just test them as functions?"

"The problem is that the data structure they emit -- the hiccup-like vectors -- are not so easy to test.
Of course I can compare an entire vector to an expected one, but this will make me duplicate the code in the test, which is something I'd rather avoid.
Furthermore, event handlers are functions, and their comparison is identity-based, meaning that even if I create the exact same event handler in the test,
it will still test different than the one created by the function I'm testing."

[[:section {:title "The Solution"}]]
"This library is intended to provide functions to extract information from hiccup-like vectors.
By being able to concisely query the returned vectors to extract the expected information we can write concise tests that exercise the component functions
outside the full context of the application.
After all, these are just functions.
We can set values to atoms, call a function extract data and assert its value.
We can extract event handlers and call them to see how they change the state, etc."

[[:section {:title "Example"}]]
"In the following example we will show a piece of Reagent code and the associated tests, based on `reagent-query`."

"The following component function takes as parameter an atom containing its state.
The state consists of a sequence of \"todo\" tasks, given as maps, each containing a unique `:id` and a `:todo` string.
The component creates a `:ul` with a `:li` for each task, with an `:input` box containing the text and a `:button` for deleting the task."

(defn todo [state]
  [:ul
   (for [{:keys [id todo]} @state]
     [:li {:key id}
      [:input {:value todo
               :on-change #(swap! state
                                  (partial map
                                           (fn [x]
                                             (if (= (:id x) id)
                                               (assoc x :todo (.-target.value %))
                                               x))))}]
      [:button {:on-click #(swap! state
                                  (partial filter
                                           (fn [x]
                                             (not= (:id x) id))))} "Done"]])])

"These are the tests:"
(fact todo-example
      ;; Empty list
      (let [state (atom [])]
        (is (= (-> (todo state)
                   (rq/query :ul :li))
               [])))

      ;; The :id field should be the :li's :key attribute
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li:key))
               [1 2])))

      ;; Each element includes an :input box with the :todo value as :value
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["One" "Two"])))

      ;; Each :li element has a :button with "Done" as text
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li :button))
               ["Done" "Done"])))

      ;; The :on-click callback associated with each button deletes the respective entry in the atom
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/query :ul :li :button:on-click))]
        ;; Let's call the second callback
        ((second callbacks))
        ;; Now we should only have "One"
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["One"])))

      ;; The :on-change callback of the :input box update the :todo of that entry
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/query :ul :li :input:on-change))]
        ;; Let's call the first callback with a mock event modifying the value to "Three"
        ((first callbacks) (rq/mock-change-event "Three"))
        ;; Now we should have "Three" instead of "One"
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["Three" "Two"]))))

[[:chapter {:title "query"}]]
"`query` is given a hiccup-like vector and zero or more additional arguments, and returns a sequence of results."

"The idea comes from [jQuery](https://jquery.com), where the `$` function (among other things) acts as a query function,
returning a sequence of results based on the given path.
Our `query` function is intended to do the same, based on a given hiccup-like vector."

"When given no extra arguments, `query` returns a single result: the given vector."
(fact q1
      (is (= (rq/query [:div "foo" "bar"]) [[:div "foo" "bar"]])))

"When given an extra keyword argument matching the element type (the first element in the vector), the child elements are returned."
(fact q2
      (is (= (rq/query [:div "foo" "bar"] :div) ["foo" "bar"])))

"If the extra parameter does not match the element type, an empty sequence is returned."
(fact q3
      (is (= (rq/query [:div "foo" "bar"] :p) [])))

"According to hiccup's convention, if the vector's second element is a map, it represents _attributes_ for the element.
If the extra argument is a keyword of the form `:elem:attr`, and if the `:elem` part matches the element,
the value of the attribute `:attr` is returned."
(fact q4
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div:id) ["x"])))

"If the second part of the keyword does not match an attribute, `nil` is added to the sequence."
(fact q5
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div:not-an-attr) [nil])))

"If the first part of the keyword does not match the element, an empty sequence is returned."
(fact q6
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :p:id) [])))

"The attribute map is ignored when returning the contents of an element."
(fact q7
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div) ["foo" "bar"])))

"If an element is a sequence (e.g., created with a `for`), we operate on all its elements and concatenate the results."
(fact q8
      (is (= (rq/query [:ul
                        [:li {:key -1}]
                        (for [i (range 3)]
                          [:li {:key i}])] :ul :li:key) [-1 0 1 2])))

[[:section {:title "Paths"}]]
"Every additional argument is applied to all the results of applying the previous argument.
For example, if we have a `:ul` element containing multiple `:li` elements, each containing a `:p` element and some text,
The path `:ul :li :p` will return a sequence of the strings."
(fact q-agg1
      (is (= (rq/query [:ul
                        [:li [:p "One"]]
                        [:li [:p "Two"]]
                        [:li [:p "Three"]]] :ul :li :p) ["One" "Two" "Three"])))

[[:section {:title "Classes"}]]
"When a path element starts with a dot (`.`), it is treated as a class matcher.
This means that it is expected to match against elements that have a `:class` attribute that includes the given class name.
For example, in the following example only \"Two\" has the class `selected`, hence it is the only one to be returned."
(fact q-cls1
      (is (= (rq/query [:ul
                        [:li {:class "other"} [:p "One"]]
                        [:li {:class "other selected"} [:p "Two"]]
                        [:li [:p "Three"]]] :ul :.selected :p) ["Two"])))

"The keyword can match at the same time against an element type and any number of classes.
For example, the keyword `:li.foo.bar` matches against `:li` elements that have both the `foo` and the `bar` classes."
(fact q-cls2
      (is (= (rq/query [:ul
                        [:li {:class "foo"} [:p "One"]]
                        [:li {:class "bar"} [:p "Two"]]
                        [:li {:class "bar foo"} [:p "Three"]]] :ul :li.foo.bar :p) ["Three"])))

"Classes can be used in conjunction with attributes."
(fact q-cls3
      (is (= (rq/query [:div
                        [:a {:href "foo.html"}]
                        [:a {:href "bar.html" :class "selected"}]
                        [:a {:href "baz.html"}]] :div :a.selected:href) ["bar.html"])))

[[:section {:title "Attribute Values"}]]
"`query` also accepts maps instead of keywords in the path (it uses [keyword-to-map](#keyword-to-map) to convert everything to maps).
For example, the selector `:a.selected:href` can be replaced with a map, as follows:"
(fact q-attr1
      (is (= (rq/query [:div
                        [:a {:href "foo.html"}]
                        [:a {:href "bar.html" :class "selected"}]
                        [:a {:href "baz.html"}]] :div {:elem "a"
                                                       :classes #{"selected"}
                                                       :attr "href"}) ["bar.html"])))

"If a map selector contains a `:attr-vals` field, every key/value pair in the given map is checked against the attributes of the element under test."
(fact q-attr2
      (is (= (rq/query [:ul
                        [:li {:foo 1} "A"]
                        [:li {:foo 3 :bar 2} "B"]
                        [:li {:foo 1 :bar 2} "C"]]
                       :ul {:elem "li"
                            :attr-vals {:foo 1 :bar 2}}) ["C"])))

"As shorthand, if a selector is a 2-element vector, its first element is [treated as a keyword](#keyword-to-map),
and its second argument is taken as the `:attr-vals` map.
The following example is exactly the same as the previous one, only that it uses the shorthand notation."
(fact q-attr3
      (is (= (rq/query [:ul
                        [:li {:foo 1} "A"]
                        [:li {:foo 3 :bar 2} "B"]
                        [:li {:foo 1 :bar 2} "C"]]
                       :ul [:li {:foo 1 :bar 2}]) ["C"])))

[[:chapter {:title "mock-change-event"}]]
"`mock-change-event` is a conveniece function that creates a mock `:on-change` event.
The function takes as parameter a new value, and generates a Javascript object that has a single member: `target`,
which by itself is a Javascript object with one field: `value`, containing the given value."
(fact mce1
      (let [ev (rq/mock-change-event "val")]
        (is (= (.-target.value ev) "val"))))

[[:chapter {:title "Under the Hood"}]]
[[:section {:title "keyword-to-map"}]]
"Under the hood, `reagent-query` paths consist of maps with the following optional fields:
1. `:elem`: The element name to match (a string).
2. `:classes`: A set of strings, each needs to be a class in the element.
3. `:attr`: If present, the result is the contents of the specified attribute, and not the contents of the element.
4. `:attr-vals`: A map of attributes with their respective values. If present, all attribute values must match."

"`keyword-to-map` converts a keyword to such a map, using the following rules."

"By default, a keyword maps into a map with an `:elem` field, of that value converted to a string."
(fact keyword-to-map1
      (is (= (:elem (rq/keyword-to-map :my-elem)) "my-elem")))

"When the keyword contains dots (`.`), the part of its name that precedes the first dot is considered the `:elem`, and the other parts are taken as the `:classes` set."
(fact keyword-to-map2
      (let [m (rq/keyword-to-map :foo.bar.baz)]
        (is (= (:elem m) "foo"))
        (is (= (:classes m) #{"bar" "baz"}))))

"Omitting the element makes it `nil`."
(fact keyword-to-map3
      (let [m (rq/keyword-to-map :.bar.baz)]
        (is (= (:elem m) nil))
        (is (= (:classes m) #{"bar" "baz"}))))

"When the keyword contains a colon (`:`), the right-hand-side of the colon is taken as the `:attr` field."
(fact keyword-to-map4
      (let [m (rq/keyword-to-map :foo:quux)]
        (is (= (:elem m) "foo"))
        (is (= (:attr m) "quux"))))
