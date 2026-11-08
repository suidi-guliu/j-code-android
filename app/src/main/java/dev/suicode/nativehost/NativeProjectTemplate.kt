package dev.suicode.nativehost

data class NativeProjectTemplate(val id: String,val packageId: String,val componentId: String,val title: String,val description: String,val files: Map<String,String>)
