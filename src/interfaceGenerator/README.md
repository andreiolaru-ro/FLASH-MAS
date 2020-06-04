# Interface generator

- Specification file format (YAML):
```
platformType: <type_platform: html | swing | android>
node:
  id: <id of root page>
  children: # list of child elements of the container
    - id: <id of child element>
      type: <type of child element> # default is block
    - id: <id of child element>
      type: <type of child element>
    - id: <type of child element>
      children: # list of child elements of the container / block (if children not speficied, then children is empty)
        - id: button1 # an element, a button as type, with text (press it) and its properties
          type: button
          text: press it
          properties: # list of properties (key - value) of an element
            type: submit # property type
```

- Example:
```yaml
platformType: html
node:
  id: root
  children:
    - id: child1
      type: button
    - id: button
      type: button
    - id: child2
      children:
        - id: button1
          type: button
          text: press it
          properties:
            type: submit
        - id: button2
          type: button
        - id: button3
          type: button
        - id: label1
          type: label
    - id: child3
      children:
        - id: button7
          type: button
        - id: button4
          type: button
        - id: forms
          type: form
```