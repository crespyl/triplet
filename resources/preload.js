// set up IPC to main process,
// thanks to https://stackoverflow.com/a/59814127
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('testAPI', {
	desktop: true
})

contextBridge.exposeInMainWorld('api', {
	send: (channel, data) => {
		// whitelist channels
		let validChannels = ["toMain"];
		if(validChannels.includes(channel)) {
			ipcRenderer.send(channel, data);
		}
	},
	recv: (channel, func) => {
		let validChannels = ["fromMain"];
		if(validChannels.includes(channel)) {
			// Deliberately strip event as it includes 'sender'
			ipcRenderer.on(channel, (event, ...args) => func(...args));
		}
	}
});
