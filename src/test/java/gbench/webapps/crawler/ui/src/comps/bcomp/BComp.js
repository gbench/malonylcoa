import axios from "axios";
import $ from "jquery";
import { mapGetters } from "vuex";

const BComp = {

    template: `<div class="highlight">{{name}}</div>`,

    /**
     * 
     * @returns 
     */
    data() {
        return {
            fileurl: ""
        };
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
    },

    methods: {
        /**
         * 
         * @param {*} event 
         */
        on_file_change(event) {
            const url = "/h5/api/upload";
            const formData = new window.FormData();
            const file = event.target.files[0];
            formData.append("file", file);
            axios.post(url, formData, {
                headers: { "Content-Type": "multipart/form-data" }
            }).then(e => {
                const data = e.data;
                this.fileurl = `/h5/api/readfile?file=${data.path}`;
                // alert(JSON.stringify(data));
            });
        }
    }

};

export { BComp };