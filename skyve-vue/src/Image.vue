<script>

import { unsanitiseBinding } from './support/Util';

const params = {
    id: '_n',
    modoc: '_doc',
    binding: '_b',
    width: '_w',
    height: '_h',
};

export default {
    props: {
        id: String,
        module: String,
        document: String,
        size: {
            type: Number,
            default: 64
        },
        binding: String
    },
    data() {
        return {
        };
    },
    computed: {
        modoc() {
            return this.module + '.' + this.document;
        },
        srcUrl() {
            const searchParams = new URLSearchParams();
            searchParams.append(params.id, this.id);
            searchParams.append(params.modoc, this.modoc);
            searchParams.append(params.binding, this.rectifiedBinding);

            if (!!this.size) {
                searchParams.append(params.width, this.size);
                searchParams.append(params.height, this.size);
            }

            return `content?${searchParams}`;
        },
        rectifiedBinding() {
            // Convert from the SmartClient sanitised binding into the
            // original binding value for content servlet
            return unsanitiseBinding(this.binding);
        }
    },
    methods: {
    }

}

</script>

<template>
    <img
        v-if="!!id"
        :src="srcUrl"
        class="data-grid-image"
    />
</template>

<style scoped>
.data-grid-image {
    max-width: 100px;
    max-height: 100px;
}
</style>