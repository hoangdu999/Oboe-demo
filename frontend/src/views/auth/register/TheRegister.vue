<template>
    <div class="the-register" :style="`background-image: url(${ImagePaths.login.loginBackground})`" >
      <div class="auth-content">
        <FormAuthen :isRegister="true"/>
      </div>
    </div>
  </template>
  
<script setup>
  import { ImagePaths } from '@/assets/img/imagePaths';
  import '@/views/auth/register/TheRegister.scss';
  import FormAuthen from '@/components/layout/form-login/FormAuthen.vue';
</script>


  