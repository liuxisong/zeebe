import React, {Component, Fragment} from 'react';
import PropTypes from 'prop-types';

import Content from 'modules/components/Content';

import withSharedState from 'modules/components/withSharedState';
import SplitPane from 'modules/components/SplitPane';
import Diagram from 'modules/components/Diagram';
import {DEFAULT_FILTER} from 'modules/constants';
import {
  fetchWorkflowInstanceBySelection,
  fetchWorkflowInstancesCount
} from 'modules/api/instances';

import {
  parseFilterForRequest,
  getFilterQueryString
} from 'modules/utils/filter';

import {getSelectionById} from 'modules/utils/selection';

import Header from '../Header';
import ListView from './ListView';
import Filters from './Filters';
import Selections from './Selections';

import sortArrayByKey from 'modules/utils/sortArrayByKey';

import {
  parseQueryString,
  createNewSelectionFragment,
  getPayload,
  decodeFields
} from './service';
import * as Styled from './styled.js';

class Instances extends Component {
  static propTypes = {
    getStateLocally: PropTypes.func.isRequired,
    storeStateLocally: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired
  };

  constructor(props) {
    super(props);
    const {
      filterCount,
      instancesInSelectionsCount,
      rollingSelectionIndex,
      selectionCount,
      selections
    } = props.getStateLocally();

    this.state = {
      activityIds: [],
      filter: {},
      filterCount: filterCount || 0,
      instancesInSelectionsCount: instancesInSelectionsCount || 0,
      openSelection: null,
      rollingSelectionIndex: rollingSelectionIndex || 0,
      selection: createNewSelectionFragment(),
      selectionCount: selectionCount || 0,
      selections: selections || [],
      workflow: null
    };
  }

  async componentDidMount() {
    this.setFilterFromUrl();
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevProps.location.search !== this.props.location.search) {
      this.setFilterFromUrl();
    }
  }

  handleStateChange = change => {
    this.setState(change);
  };

  addSelectionToList = selection => {
    const {
      rollingSelectionIndex,
      instancesInSelectionsCount,
      selectionCount
    } = this.state;

    const currentSelectionIndex = rollingSelectionIndex + 1;

    // Add Id for each selection
    this.setState(
      prevState => ({
        selections: [
          {
            selectionId: currentSelectionIndex,
            ...selection
          },
          ...prevState.selections
        ],
        rollingSelectionIndex: currentSelectionIndex,
        instancesInSelectionsCount:
          instancesInSelectionsCount + selection.totalCount,
        selectionCount: selectionCount + 1
      }),
      () => {
        const {
          selections,
          rollingSelectionIndex,
          instancesInSelectionsCount,
          selectionCount
        } = this.state;
        this.props.storeStateLocally({
          selections,
          rollingSelectionIndex,
          instancesInSelectionsCount,
          selectionCount
        });
      }
    );
  };

  handleAddNewSelection = async () => {
    const payload = getPayload({state: this.state});
    const instancesDetails = await fetchWorkflowInstanceBySelection(payload);
    this.addSelectionToList({...payload, ...instancesDetails});
  };

  handleAddToSelectionById = async selectionId => {
    const {selections} = this.state;
    const selectiondata = getSelectionById(selections, selectionId);
    const payload = getPayload({state: this.state, selectionId});
    const instancesDetails = await fetchWorkflowInstanceBySelection(payload);

    const newSelection = {
      ...selections[selectiondata.index],
      ...payload,
      ...instancesDetails
    };

    selections[selectiondata.index] = newSelection;

    this.setState(selections);
    this.props.storeStateLocally({
      selections
    });
  };

  setFilterFromUrl = () => {
    let {filter} = parseQueryString(this.props.location.search);

    // filter from URL was missing or invalid
    if (!filter) {
      // set default filter selection
      filter = DEFAULT_FILTER;
      this.setFilterInURL(filter);
    }

    this.setState({filter: {...decodeFields(filter)}}, () => {
      this.handleFilterCount();
    });
  };

  handleFilterCount = async () => {
    const filterCount = await fetchWorkflowInstancesCount(
      parseFilterForRequest(this.state.filter)
    );

    this.setState({
      filterCount
    });

    // save filterCount to localStorage
    this.props.storeStateLocally({filterCount});
  };

  handleFilterChange = async newFilter => {
    const filter = {...this.state.filter, ...newFilter};
    this.setFilterInURL(filter);

    // write current filter selection to local storage
    this.props.storeStateLocally({filter: filter});
  };

  handleWorkflowChange = workflow => {
    this.setState({workflow});
  };

  setFilterInURL = filter => {
    this.props.history.push({
      pathname: this.props.location.pathname,
      search: getFilterQueryString(filter)
    });
  };

  handleFilterReset = () => {
    this.setFilterInURL(DEFAULT_FILTER);

    // reset filter in local storage
    this.props.storeStateLocally({filter: DEFAULT_FILTER});
  };

  handleFlowNodesDetailsReady = nodes => {
    let activityIds = [];
    let node;
    for (node in nodes) {
      if (nodes[node].type === 'TASK') {
        activityIds.push({
          value: node,
          label: nodes[node].name || 'Unnamed task'
        });
      }
    }

    this.setState({
      activityIds: sortArrayByKey(activityIds, 'label')
    });
  };

  render() {
    const {running, incidents: incidentsCount} = this.props.getStateLocally();
    return (
      <Fragment>
        <Header
          active="instances"
          instances={running}
          filters={this.state.filterCount}
          selections={0} // needs a backend call because selections are complex
          incidents={incidentsCount}
        />
        <Content>
          <Styled.Instances>
            <Styled.Filters>
              <Filters
                filter={this.state.filter}
                activityIds={this.state.activityIds}
                onFilterReset={this.handleFilterReset}
                onFilterChange={this.handleFilterChange}
                onWorkflowVersionChange={this.handleWorkflowChange}
              />
            </Styled.Filters>

            <Styled.Center>
              <SplitPane.Pane isRounded>
                <SplitPane.Pane.Header isRounded>
                  {this.state.workflow
                    ? this.state.workflow.name || this.state.workflow.id
                    : 'Workflow'}
                </SplitPane.Pane.Header>
                <SplitPane.Pane.Body>
                  {this.state.workflow && (
                    <Diagram
                      workflowId={this.state.workflow.id}
                      onFlowNodesDetailsReady={this.handleFlowNodesDetailsReady}
                    />
                  )}
                </SplitPane.Pane.Body>
              </SplitPane.Pane>

              <ListView
                openSelection={this.state.openSelection}
                filterCount={this.state.filterCount}
                onUpdateSelection={change => {
                  this.handleStateChange({selection: {...change}});
                }}
                selection={this.state.selection}
                selections={this.state.selections}
                filter={this.state.filter}
                errorMessage={this.state.errorMessage}
                onAddNewSelection={this.handleAddNewSelection}
                onAddToSpecificSelection={this.handleAddToSelectionById}
                onAddToOpenSelection={() =>
                  this.handleAddToSelectionById(this.state.openSelection)
                }
              />
            </Styled.Center>
            <Selections
              openSelection={this.state.openSelection}
              selections={this.state.selections}
              rollingSelectionIndex={this.state.rollingSelectionIndex}
              selectionCount={this.state.selectionCount}
              instancesInSelectionsCount={this.state.instancesInSelectionsCount}
              filter={this.state.filter}
              storeStateLocally={this.props.storeStateLocally}
              onStateChange={this.handleStateChange}
            />
          </Styled.Instances>
        </Content>
      </Fragment>
    );
  }
}

const WrappedInstances = withSharedState(Instances);
WrappedInstances.WrappedComponent = Instances;

export default WrappedInstances;
