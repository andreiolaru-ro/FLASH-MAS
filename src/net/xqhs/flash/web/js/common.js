/** The state of the application, shared across all modules */
export let appContext = {
    entities: {},
    selectedEntities: [],
};

/** Constructs the id of an element from entity, port and role */
export const idOf = (entity, port, role) => CSS.escape(`${entity}_${port}_${role}`);

/** Extend the jQuery API to include a shorthand for setting id */
$.fn.id = function(id) {
    return this.attr('id', id);
}