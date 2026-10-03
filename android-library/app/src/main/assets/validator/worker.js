import {validate} from './validate.js';
self.onmessage=async event=>{self.postMessage(await validate(event.data));};
