



* whether an entity allows a stop operation or not is its choice (via the remoteOperation shard). In any case, it can be killed via the node.
* passive inputs work only locally. Any remote interfaces must use the active input functionality, by which an actual event (a wave) is generated and sent to the entity. The GuiShard must handle updating any fields which are considered passive inputs, in all local or remote interfaces, when the input is updated.