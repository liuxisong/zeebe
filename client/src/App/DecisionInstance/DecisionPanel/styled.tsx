/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import styled, {css} from 'styled-components';

const Container = styled.div`
  ${({theme}) => {
    return css`
      background: ${theme.colors.decisionViewer.background};
      overflow: auto;
      height: 100%;
    `;
  }}
`;
const IncidentBanner = styled.div`
  ${({theme}) => {
    return css`
      background-color: ${theme.colors.incidentsAndErrors};
      color: ${theme.colors.white};
      font-size: 15px;
      font-weight: 500;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
    `;
  }}
`;

export {IncidentBanner, Container};
