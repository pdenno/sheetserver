# sheetserver

An exploratory program to serve content from a spreadsheet using an Open API interface

Its purpose is to be like training wheels for someone who wants to experiment with service-orientation and Open API.

## Installation

### Install for development

(1) Install [leiningen](https://leiningen.org/) as described on that page.

(2) Download this repository (https://github.com/pdenno/sheetserver) 

(3) Execute `lein run` in the directory in which you downloaded the repository.

(4) Navigate to http://localhost:8855/sheetserver/ok . You should get an 'ok'

(5) Any further investigation assumes you are willing to learn a little about [https://clojure.org/](Clojure). Above all, have fun!

### Install for non-developer usage

Ask for a jar file. 

## Running as a non-developer

```
Usage: java -jar sheetserver-0.1.0-standalone.jar [--port <port num>]
                                                  [--mapping <mapping file>]
                                                  [--schema <schema file>]
                                                  [--spreadsheet <spreadsheet file>]
                                                  [--sheet <name of sheet in spreasheet file>]
```

Any of the optional arguments not specified default to the example usage in the resources directory.
Typical usage is one of (1) run the example:(use no args), (2) specify just the --port, and (3) specify all args
except maybe keep the default port (8855).

* `--port` a port number.
* `--mapping` a mapping file, as described below.
* `--schema` an Open API specification. See `resources/warehouse.json` or `resources/warehouse.yaml` for an example.
* `--spreadsheet` an spreadsheet (.xlsx file).
* `--sheet` a name of a 

### Running as a developer

```
(in-ns 'pdenno.sheetserver.core)
(start-server)

; Jul 21, 2019 4:01:53 PM clojure.tools.logging$eval454$fn__457 invoke
; INFO: Server starting on port 8855
```
A keyword can be substitued for a command-line switch. Thus `(start-server :port 8989)`. 


## Examples

Start the server with no command-line args; the example spreadsheet and mapping spec will be used. 
The example mapping in resources directory, `sheet2uri-map.json`, contains only one property to 
map. Navigate to http://localhost:8855/warehouse/matl-onhand?item-id=BIND-ARAM . It should return `2899.0`. 

So what happened? Read the next section, 'The Mapping File'.

## The Mapping File

The mapping file is a JSON file that specifies the relationship between the data in spreadsheet
and URIs to access the data secified in the map. The example contains one map that looks like this:

```javascript
{"path"      :  "/warehouse/matl-onhand",
 "method"    :  "get"
 "type"      :  "at-most-one"
 "map"       :   {"key"   :  {"column"          :  "A",
                              "query-param"     :  "item-id"
                              "user"            :  "Material Item No.",
                              "open-api-model"  :  "Item.id", },
                  "value" :  {"column"          :  "B",
                              "user"            :  "O.H. Qty.",
                              "open-api-model"  :  "Item.onhand-quantity"}}}
```				

 * `path` is the URI sans query string.
 * `method` is the http method to be invoked. (`get` is currently the only choice.)
 * `map` starts the map specification, described in the next bullets.
 * `type` `at-most-one` (currently the only choice) stipulates that the relationship between the key and value is 1-1.
 * `column` (in both `key` and `value`) specifies the column (A,B,C,..). where the data property is found; conventional office tool nomenclature.
 * `open-api-model` is a reference to this property in the corresponding Open API spec. (resources/warehouse.json in the example.)
 * `user` is an optional user name for the property. It could be the column heading in the spreadsheet.
 * `query-param` is the query-string parameter to be used in the URI. 
 
Thus in this example a valid URI path is `/warehouse/matl-onhand?item-id=whatever`. 

### Limitations

(1) The only http method currently supported is GET.

(2) The GET uses only one query parameter; this parameter's value selects the spreadsheet row, 
the URI path as described in the mapping, selects the cell.

(3) The Open API file isn't used yet. We'll link it in for use with spec checking and swagger-ui later. 

## License

Copyright © 2019 Peter Denno

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
