import { mapGetters } from "vuex";

const BComp = {

    template: `<div class="highlight">{{name}}</div>`,

    /**
     * 
     * @returns 
     */
    data() {
        return {};
    },

    /**
     * 
     */
    mounted() {
        console.log(this.name);
    },

    /**
     * 
     */
    computed: {
        /**
         * Getters 数据
         */
        ...mapGetters("BCompStore", ["name"]),
    }

};

export { BComp };