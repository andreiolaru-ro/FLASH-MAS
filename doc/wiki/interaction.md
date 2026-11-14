**Note: this only applies to the nested-entity model, and even there there may be upcoming changes**

In the current model, **agents** interact via **messages**. Any other interaction (e.g. between shards in an agent) is performed either via an agent (such as for monitoring activities) or via direct calls to the methods of an entity or an entitiy's proxy (such as when shards interact).

Messages have a string **content**, have a **source** and a **destination** (also strings).

TODO: take from paper
